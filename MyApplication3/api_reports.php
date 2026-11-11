<?php
ob_start();
require_once 'db.php';

$method = $_SERVER['REQUEST_METHOD'];

switch ($method) {
    case 'GET':
        // Fetch all reports and include the filer's name
        $query = "SELECT r.*, u.name as filed_by_user FROM reports r LEFT JOIN users u ON r.user_id_who_filed = u.id ORDER BY r.id DESC";
        $stmt = $pdo->query($query);
        $reports = $stmt->fetchAll();
        ob_clean();
        echo json_encode(["status" => "success", "data" => $reports]);
        break;

    case 'POST':
        $content = file_get_contents("php://input");
        $data = json_decode($content, true);
        if (is_null($data)) { $data = $_POST; }

        if (empty($data['user_id_who_filed']) || empty($data['location'])) {
            http_response_code(400);
            echo json_encode(["status" => "error", "message" => "Missing required fields"]);
            exit();
        }

        try {
            // Verify user exists in DB, otherwise fallback to prevent foreign key integrity error
            $user_id = $data['user_id_who_filed'];
            $userCheck = $pdo->prepare("SELECT id FROM users WHERE id = ?");
            $userCheck->execute([$user_id]);
            if (!$userCheck->fetch()) {
                $fallbackUser = $pdo->query("SELECT id FROM users ORDER BY id ASC LIMIT 1")->fetch();
                if ($fallbackUser) {
                    $user_id = $fallbackUser['id'];
                } else {
                    $pdo->exec("INSERT INTO users (name, username, email, password) VALUES ('Guest', 'guest@reunite.com', 'guest@reunite.com', 'password')");
                    $user_id = $pdo->lastInsertId();
                }
            }

            $image_name = null;
            if (!empty($data['encoded_image'])) {
                $encoded_image = $data['encoded_image'];

                // Remove potential Base64 metadata prefix if present
                if (preg_match('/^data:image\/(\w+);base64,/', $encoded_image, $type)) {
                    $encoded_image = substr($encoded_image, strpos($encoded_image, ',') + 1);
                }

                $image_data = base64_decode($encoded_image);
                // Use uniqid to prevent overwriting files if uploaded at the same time
                $image_name = 'img_' . time() . '_' . uniqid() . '.jpg';

                if (!file_exists('uploads')) {
                    mkdir('uploads', 0777, true);
                }

                file_put_contents('uploads/' . $image_name, $image_data);
            }

            // Combine Description and Location so all information is saved in the DB
            $final_location = $data['location'];
            if (!empty($data['description'])) {
                $final_location = "[" . $data['description'] . "] " . $final_location;
            }

            $sql = "INSERT INTO reports
                    (report_id, user_id_who_filed, date_filed, photo_image, location)
                    VALUES (:report_id, :user_id, NOW(), :photo, :loc)";

            $stmt = $pdo->prepare($sql);
            $stmt->execute([
                'report_id' => ($data['report_type'] ?? 'ITEM') . '-' . strtoupper(substr(uniqid(), -5)),
                'user_id'   => $user_id,
                'photo'     => $image_name,
                'loc'       => $final_location
            ]);

            ob_clean();
            echo json_encode(["status" => "success", "message" => "Report filed successfully"]);
            exit();
        } catch (PDOException $e) {
            ob_clean();
            http_response_code(500);
            echo json_encode(["status" => "error", "message" => "Database Error: " . $e->getMessage()]);
            exit();
        }
        break;
}
?>
