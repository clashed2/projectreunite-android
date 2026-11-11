<?php
ob_start();
require_once 'db.php';

$content = file_get_contents("php://input");
$data = json_decode($content, true);

if (is_null($data)) {
    $data = $_POST;
}

$username = $data['username'] ?? '';
$password = $data['password'] ?? '';

if (!empty($username) && !empty($password)) {
    $stmt = $pdo->prepare("SELECT * FROM users WHERE username = :username OR email = :username LIMIT 1");
    $stmt->execute(['username' => $username]);
    $user = $stmt->fetch();

    if ($user && password_verify($password, $user['password'])) {
        unset($user['password']);
        ob_clean();
        echo json_encode(["status" => "success", "user" => $user]);
    } else {
        ob_clean();
        http_response_code(401);
        echo json_encode(["status" => "error", "message" => "Invalid credentials"]);
    }
} else {
    ob_clean();
    http_response_code(400);
    echo json_encode(["status" => "error", "message" => "Username and password required"]);
}
