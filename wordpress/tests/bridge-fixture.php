<?php
// HTTP fixture for the bridge's request contract. WordPress identity and the
// database are simulated. This router must never be deployed on the website.
if ( PHP_SAPI !== 'cli-server' ) {
    exit( 1 );
}

define( 'ABSPATH', __DIR__ );
function add_action( ...$args ) {}
function add_filter( ...$args ) {}
function register_activation_hook( ...$args ) {}
function register_deactivation_hook( ...$args ) {}
function wp_unslash( $value ) { return $value; }
function is_ssl() { return true; }
function home_url( $path, $scheme ) { return 'http://' . $_SERVER['HTTP_HOST'] . $path; }
function wc_get_page_permalink( $name ) { return home_url( '/my-account/', 'https' ); }
function add_query_arg( $args, ...$rest ) {
    $url = is_array( $args ) ? $rest[0] : $rest[1];
    $params = is_array( $args ) ? $args : array( $args => $rest[0] );
    $pairs = array();
    foreach ( $params as $key => $value ) $pairs[] = $key . '=' . $value;
    return $url . ( strpos( $url, '?' ) === false ? '?' : '&' ) . implode( '&', $pairs );
}
function nocache_headers() {}
function status_header( $code ) { http_response_code( $code ); }
function esc_html( $value ) { return htmlspecialchars( $value, ENT_QUOTES ); }
function esc_attr( $value ) { return esc_html( $value ); }
function esc_url( $value ) { return esc_attr( $value ); }
function wp_safe_redirect( $url, $status, $source ) { header( 'Location: ' . $url, true, $status ); }
function get_option( $name ) { return '1'; }
function get_current_user_id() { return (int) ( $_COOKIE['fixture_user'] ?? 0 ); }
function is_user_logged_in() { return get_current_user_id() > 0; }
function is_super_admin( $id ) { return $id === 9; }
function user_can( $user, $capability ) { return $user->ID === 9; }
function wp_get_current_user() { return new WP_User( get_current_user_id() ); }
function get_user_by( $field, $id ) { return new WP_User( $id ); }
function wp_logout_url( $target ) { return $target; }
function wp_nonce_field( $action, $name, ...$args ) { return '<input name="' . $name . '" value="fixture-nonce">'; }
function wp_verify_nonce( $nonce, $action ) { return $nonce === 'fixture-nonce'; }
function wp_clear_auth_cookie() {}
function wp_destroy_current_session() {}
function wp_set_current_user( $id ) {}
function wp_set_auth_cookie( $id, $remember, $secure ) {
    setcookie( 'fixture_user', (string) $id, array( 'path' => '/', 'httponly' => true ) );
}

class WP_User {
    public $ID;
    public $display_name = 'Fixture student';
    public $user_email = 'student@example.invalid';
    public $roles;
    function __construct( $id ) {
        $this->ID = $id;
        $this->roles = array( $id === 9 ? 'administrator' : 'customer' );
    }
    function exists() { return $this->ID > 0; }
}

class BridgeDatabase {
    public $prefix = 'test_';
    public function prepare( $sql, ...$params ) { return array( $sql, $params ); }
    public function get_var( $statement ) { return $this->query( $statement ); }
    public function query( $statement ) {
        list( $sql, $params ) = $statement;
        $path = getenv( 'CT_TEST_DB' );
        $file = fopen( $path, 'c+' );
        flock( $file, LOCK_EX );
        $rows = json_decode( stream_get_contents( $file ), true ) ?? array();
        $result = $this->execute( $sql, $params, $rows );
        rewind( $file );
        ftruncate( $file, 0 );
        fwrite( $file, json_encode( $rows ) );
        fclose( $file );
        return $result;
    }
    private function execute( $sql, $params, &$rows ) {
        if ( strpos( $sql, 'INSERT IGNORE' ) === 0 ) {
            list( $state, $challenge, $browser, $expires ) = $params;
            if ( isset( $rows[$state] ) ) return 0;
            $rows[$state] = array( 'challenge' => $challenge, 'browser' => $browser, 'expires' => $expires, 'id' => 0, 'code' => '', 'used' => 0 );
            return 1;
        }
        if ( strpos( $sql, 'SET code_hash' ) !== false ) {
            list( $code, $user, $expires, $state, $browser, $now ) = $params;
            $row = $rows[$state] ?? null;
            if ( ! $row || $row['browser'] !== $browser || $row['expires'] <= $now || $row['id'] !== 0 ) return 0;
            $rows[$state]['code'] = $code;
            $rows[$state]['id'] = $user;
            $rows[$state]['expires'] = $expires;
            return 1;
        }
        if ( strpos( $sql, 'challenge = ' ) !== false ) {
            $consume = strpos( $sql, 'SET consumed' ) !== false;
            list( $state, $challenge, $code ) = $params;
            $now = $params[$consume ? 4 : 3];
            $row = $rows[$state] ?? null;
            if ( ! $row || $row['expires'] <= $now || $row['used'] || $row['challenge'] !== $challenge || $row['code'] !== $code ) return 0;
            if ( ! $consume ) return $row['id'];
            if ( $row['id'] !== $params[3] ) return 0;
            $rows[$state]['used'] = 1;
            return 1;
        }
        list( $state, $match, $now ) = $params;
        $row = $rows[$state] ?? null;
        if ( ! $row || $row['expires'] <= $now ) return 0;
        if ( strpos( $sql, 'browser_hash = ' ) !== false ) return (int) ( $row['browser'] === $match && $row['id'] === 0 );
        return (int) ( $row['id'] === $match && $row['used'] );
    }
}

$wpdb = new BridgeDatabase();
require dirname( __DIR__ ) . '/ct-app-login-bridge/ct-app-login-bridge.php';
CrackTarget\AndroidLogin\dispatch();
echo is_user_logged_in() ? 'Signed in student ' . get_current_user_id() : 'Signed out';
