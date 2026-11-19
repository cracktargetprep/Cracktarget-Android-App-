<?php
/**
 * Plugin Name: CrackTarget Android Login Bridge
 * Description: Connects a browser sign-in to a new WooCommerce session in the CrackTarget Android app.
 * Version: 2.0.1
 * Requires PHP: 7.4
 */

namespace CrackTarget\AndroidLogin;

if ( ! defined( 'ABSPATH' ) ) {
    exit;
}

const SCHEMA_VERSION = '1';
const LIFETIME = 600;
const CODE_LIFETIME = 120;
const BROWSER_COOKIE = '__Host-ct_android_login';

function table() {
    global $wpdb;
    return $wpdb->prefix . 'ct_android_login';
}

function install() {
    global $wpdb;
    require_once ABSPATH . 'wp-admin/includes/upgrade.php';
    $name = table();
    $charset = $wpdb->get_charset_collate();
    dbDelta( "CREATE TABLE $name (
        state char(64) NOT NULL,
        challenge char(64) NOT NULL,
        browser_hash char(64) NOT NULL,
        code_hash char(64) NOT NULL DEFAULT '',
        user_id bigint unsigned NOT NULL DEFAULT 0,
        expires bigint unsigned NOT NULL,
        consumed tinyint unsigned NOT NULL DEFAULT 0,
        PRIMARY KEY  (state),
        KEY expires (expires)
    ) $charset;" );
    if ( $wpdb->get_var( $wpdb->prepare( 'SHOW TABLES LIKE %s', $wpdb->esc_like( $name ) ) ) === $name ) {
        update_option( 'ct_android_login_schema', SCHEMA_VERSION, false );
    }
    if ( ! wp_next_scheduled( 'ct_android_login_cleanup' ) ) {
        wp_schedule_event( time() + HOUR_IN_SECONDS, 'hourly', 'ct_android_login_cleanup' );
    }
}

register_activation_hook( __FILE__, __NAMESPACE__ . '\\install' );
register_deactivation_hook( __FILE__, static function () {
    wp_clear_scheduled_hook( 'ct_android_login_cleanup' );
} );
add_action( 'ct_android_login_cleanup', static function () {
    global $wpdb;
    $wpdb->query( $wpdb->prepare( 'DELETE FROM ' . table() . ' WHERE expires <= %d', time() ) );
} );

function input( array $source, $key ) {
    return isset( $source[ $key ] ) && is_string( $source[ $key ] ) ? wp_unslash( $source[ $key ] ) : '';
}

function valid_token( $value ) {
    return preg_match( '/\A[a-f0-9]{64}\z/', $value ) === 1;
}

function endpoint( $action, $state = '' ) {
    $args = array( 'ctapp_v2' => $action );
    if ( $state !== '' ) {
        $args['state'] = $state;
    }
    return add_query_arg( $args, home_url( '/', 'https' ) );
}

function account_url() {
    return wc_get_page_permalink( 'myaccount' );
}

function browser_token( $state ) {
    $parts = explode( '.', input( $_COOKIE, BROWSER_COOKIE ), 2 );
    return count( $parts ) === 2 && hash_equals( $state, $parts[0] ) && valid_token( $parts[1] ) ? $parts[1] : '';
}

function browser_state() {
    $parts = explode( '.', input( $_COOKIE, BROWSER_COOKIE ), 2 );
    return count( $parts ) === 2 && valid_token( $parts[0] ) ? $parts[0] : '';
}

function set_browser_cookie( $value, $expires ) {
    setcookie( BROWSER_COOKIE, $value, array(
        'expires' => $expires,
        'path' => '/',
        'secure' => true,
        'httponly' => true,
        'samesite' => 'Lax',
    ) );
}

function customer( $user ) {
    return $user instanceof \WP_User && $user->exists() && ! is_super_admin( $user->ID )
        && ! user_can( $user, 'manage_woocommerce' ) && ! user_can( $user, 'edit_posts' )
        && count( array_diff( $user->roles, array( 'customer', 'subscriber' ) ) ) === 0
        && count( $user->roles ) > 0;
}

function security_headers() {
    nocache_headers();
    header( 'Cache-Control: private, no-store, max-age=0' );
    header( 'Referrer-Policy: no-referrer' );
    header( 'X-Content-Type-Options: nosniff' );
    header( "Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'" );
    header( 'X-Robots-Tag: noindex, nofollow' );
}

function page( $title, $body, $status = 200 ) {
    status_header( $status );
    header( 'Content-Type: text/html; charset=UTF-8' );
    echo '<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">';
    echo '<title>' . esc_html( $title ) . ' | CrackTarget</title><style>body{font:17px/1.5 system-ui,sans-serif;background:#f6f7fb;color:#102544;padding:24px}main{background:#fff;margin:8vh auto;max-width:480px;padding:28px;border-radius:16px}a,button{display:inline-block;background:#102544;color:white;border:0;border-radius:8px;padding:12px 18px;font:inherit;text-decoration:none;cursor:pointer}p{overflow-wrap:anywhere}</style></head><body><main>';
    echo '<h1>' . esc_html( $title ) . '</h1>' . $body . '</main></body></html>'; // Body is assembled from literals and escaped values below.
    exit;
}

function redirect( $url ) {
    wp_safe_redirect( $url, 303, 'CrackTarget Android Login' );
    exit;
}

function expired( $status = 403 ) {
    page( 'Start sign-in again', '<p>Open CrackTarget and tap Sign in with Google to start a new request.</p>', $status );
}

add_action( 'template_redirect', __NAMESPACE__ . '\\dispatch', 0 );
function dispatch() {
    global $wpdb;
    if ( ! isset( $_GET['ctapp_v2'] ) ) {
        return;
    }
    security_headers();
    if ( ! is_ssl() || ! function_exists( 'wc_get_page_permalink' ) ) {
        expired( 503 );
    }
    $action = input( $_GET, 'ctapp_v2' );
    $method = $_SERVER['REQUEST_METHOD'] ?? '';
    if ( $method !== ( in_array( $action, array( 'approve', 'exchange' ), true ) ? 'POST' : 'GET' ) ) {
        expired( 405 );
    }
    $state = input( $_GET, 'state' );
    if ( ! valid_token( $state ) ) {
        expired( 400 );
    }

    if ( get_option( 'ct_android_login_schema' ) !== SCHEMA_VERSION ) {
        expired( 503 );
    }
    $name = table();
    $now = time();
    if ( $action === 'start' ) {
        $challenge = input( $_GET, 'challenge' );
        if ( ! valid_token( $challenge ) ) {
            expired( 400 );
        }
        $browser = bin2hex( random_bytes( 32 ) );
        $added = $wpdb->query( $wpdb->prepare(
            "INSERT IGNORE INTO $name (state, challenge, browser_hash, expires) VALUES (%s, %s, %s, %d)",
            $state, $challenge, hash( 'sha256', $browser ), $now + LIFETIME
        ) );
        if ( $added !== 1 ) {
            expired( 409 );
        }
        set_browser_cookie( $state . '.' . $browser, $now + LIFETIME );
        redirect( endpoint( 'confirm', $state ) );
    }

    if ( $action === 'confirm' || $action === 'approve' ) {
        $browser = browser_token( $state );
        $browser_hash = hash( 'sha256', $browser );
        $pending = $browser !== '' && $wpdb->get_var( $wpdb->prepare(
            "SELECT 1 FROM $name WHERE state = %s AND browser_hash = %s AND expires > %d AND user_id = 0",
            $state, $browser_hash, $now
        ) );
        if ( ! $pending ) {
            expired();
        }
        if ( ! is_user_logged_in() ) {
            if ( $action === 'approve' ) {
                expired();
            }
            redirect( add_query_arg( 'redirect_to', rawurlencode( endpoint( 'confirm', $state ) ), account_url() ) );
        }
        $user = wp_get_current_user();
        if ( ! customer( $user ) ) {
            page( 'Use your student account', '<p>This browser is signed in to a different type of CrackTarget account. Sign out in the browser, then start again in the app.</p><p><a href="' . esc_url( wp_logout_url( account_url() ) ) . '">Sign out of the browser</a></p>', 403 );
        }
        if ( $action === 'confirm' ) {
            $button = '<form method="post" action="' . esc_url( endpoint( 'approve', $state ) ) . '">';
            $button .= wp_nonce_field( 'ct_android_approve_' . $state, 'ct_android_nonce', false, false );
            $button .= '<button type="submit">Continue as ' . esc_html( $user->display_name ) . '</button></form>';
            page( 'Continue to CrackTarget', '<p>Use this CrackTarget account in the app only if you just started sign-in there.</p><p>' . esc_html( $user->user_email ) . '</p>' . $button . '<p><a href="' . esc_url( wp_logout_url( account_url() ) ) . '">Use another account</a></p>' );
        }
        if ( ! wp_verify_nonce( input( $_POST, 'ct_android_nonce' ), 'ct_android_approve_' . $state ) ) {
            expired();
        }
        $code = bin2hex( random_bytes( 32 ) );
        $approved = $wpdb->query( $wpdb->prepare(
            "UPDATE $name SET code_hash = %s, user_id = %d, expires = %d WHERE state = %s AND browser_hash = %s AND expires > %d AND user_id = 0",
            hash( 'sha256', $code ), $user->ID, $now + CODE_LIFETIME, $state, $browser_hash, $now
        ) );
        if ( $approved !== 1 ) {
            expired();
        }
        set_browser_cookie( '', $now - 3600 );
        $intent = 'intent://auth/complete?state=' . $state . '&code=' . $code . '#Intent;scheme=ctapp;package=com.cracktarget.app;end';
        $direct = 'ctapp://auth/complete?state=' . $state . '&code=' . $code;
        page( 'Return to CrackTarget', '<p>Open the app to finish signing in.</p><p><a href="' . esc_attr( $intent ) . '">Open CrackTarget</a></p><p><a href="' . esc_attr( $direct ) . '">Try another way to open the app</a></p>' );
    }

    if ( $action === 'exchange' ) {
        $code = input( $_POST, 'code' );
        $verifier = input( $_POST, 'verifier' );
        if ( ! valid_token( $code ) || ! valid_token( $verifier ) ) {
            expired( 400 );
        }
        $code_hash = hash( 'sha256', $code );
        $challenge = hash( 'sha256', $verifier );
        $user_id = (int) $wpdb->get_var( $wpdb->prepare(
            "SELECT user_id FROM $name WHERE state = %s AND challenge = %s AND code_hash = %s AND expires > %d AND consumed = 0",
            $state, $challenge, $code_hash, $now
        ) );
        $user = $user_id ? get_user_by( 'id', $user_id ) : false;
        $current = get_current_user_id();
        if ( ! customer( $user ) || ( $current && $current !== $user_id ) ) {
            expired();
        }
        $consumed = $wpdb->query( $wpdb->prepare(
            "UPDATE $name SET consumed = 1 WHERE state = %s AND challenge = %s AND code_hash = %s AND user_id = %d AND expires > %d AND consumed = 0",
            $state, $challenge, $code_hash, $user_id, $now
        ) );
        if ( $consumed !== 1 ) {
            expired();
        }
        if ( $current ) {
            wp_destroy_current_session();
        }
        wp_clear_auth_cookie();
        wp_set_current_user( $user_id );
        wp_set_auth_cookie( $user_id, true, true );
        redirect( endpoint( 'verify', $state ) );
    }

    if ( $action === 'verify' ) {
        $current = get_current_user_id();
        $issued = $current && $wpdb->get_var( $wpdb->prepare(
            "SELECT 1 FROM $name WHERE state = %s AND user_id = %d AND expires > %d AND consumed = 1",
            $state, $current, $now
        ) );
        if ( ! $issued ) {
            expired();
        }
        redirect( add_query_arg( 'ctapp_signed_in', $state, account_url() ) );
    }
    expired( 404 );
}

// An in-place plugin update can keep the active plugin without running its
// activation hook. A privileged admin visit makes that upgrade idempotent.
add_action( 'admin_init', static function () {
    if ( current_user_can( 'activate_plugins' ) && get_option( 'ct_android_login_schema' ) !== SCHEMA_VERSION ) {
        install();
    }
} );

// This only disables Google's embedded widget in the Android WebView. The
// regular browser still uses the store's existing Site Kit configuration.
add_filter( 'googlesitekit_sign-in-with-google_tag_blocked', static function ( $blocked ) {
    return $blocked || strpos( $_SERVER['HTTP_USER_AGENT'] ?? '', ' CrackTargetAndroid/2' ) !== false;
} );

function resume_browser_login( $redirect ) {
    $state = browser_state();
    return $state === '' ? $redirect : endpoint( 'confirm', $state );
}
add_filter( 'woocommerce_login_redirect', __NAMESPACE__ . '\\resume_browser_login', PHP_INT_MAX );
add_filter( 'woocommerce_registration_redirect', __NAMESPACE__ . '\\resume_browser_login', PHP_INT_MAX );

// Existing checkout and authentication settings remain the source of truth. This
// only supplies a route back from WooCommerce for a browser with a live app request.
add_action( 'woocommerce_account_dashboard', static function () {
    $state = browser_state();
    if ( $state !== '' ) {
        echo '<p><a class="button" href="' . esc_url( endpoint( 'confirm', $state ) ) . '">Continue to the CrackTarget app</a></p>';
    }
}, 3 );

// Retire the previous GET-based exchange. Old Android builds can still sign in
// with a WooCommerce password in the web account or install the updated app.
add_action( 'template_redirect', static function () {
    if ( isset( $_GET['ctab_exchange'] ) || isset( $_GET['ctab_return'] ) ) {
        security_headers();
        page( 'Update CrackTarget', '<p>Use the updated Android app to continue with Google sign-in.</p>', 410 );
    }
}, 0 );
