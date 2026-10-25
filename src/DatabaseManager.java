
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:data/manager.db";

    public static class ThongTinClient {
        public String clientId;
        public String username;
        public String tenMay;
        public String heDieuHanh;
        public String ngayDangKy;
        public String lanCuoi;
        public String trangThai;

        public ThongTinClient(
                String clientId,
                String username,
                String tenMay,
                String heDieuHanh,
                String ngayDangKy,
                String lanCuoi,
                String trangThai) {
            this.clientId = clientId;
            this.username = username;
            this.tenMay = tenMay;
            this.heDieuHanh = heDieuHanh;
            this.ngayDangKy = ngayDangKy;
            this.lanCuoi = lanCuoi;
            this.trangThai = trangThai;
        }
    }

    public static Connection ketNoi() throws SQLException {
        Connection ketNoi = DriverManager.getConnection(DB_URL);

        try (Statement lenh = ketNoi.createStatement()) {
            lenh.execute("PRAGMA foreign_keys = ON");
        }

        return ketNoi;
    }

    public static void khoiTaoDatabase() {
        try {
            File thuMuc = new File("data");

            if (!thuMuc.exists()) {
                thuMuc.mkdirs();
            }

            try (Connection ketNoi = ketNoi();
                 Statement lenh = ketNoi.createStatement()) {

                lenh.execute("""
                    CREATE TABLE IF NOT EXISTS clients (
                        client_id TEXT PRIMARY KEY,
                        username TEXT NOT NULL,
                        computer_name TEXT,
                        operating_system TEXT,
                        registered_at TEXT DEFAULT CURRENT_TIMESTAMP,
                        last_seen TEXT,
                        status TEXT DEFAULT 'Offline'
                    )
                """);

                lenh.execute("""
                    CREATE TABLE IF NOT EXISTS activity_history (
                        history_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        client_id TEXT NOT NULL,
                        event_type TEXT NOT NULL,
                        description TEXT,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (client_id)
                            REFERENCES clients(client_id)
                            ON DELETE CASCADE
                    )
                """);

                System.out.println("Ket noi database thanh cong!");
                System.out.println("Duong dan database: "
                        + new File("data/manager.db").getAbsolutePath());
            }

        } catch (SQLException e) {
            System.out.println("Loi database: " + e.getMessage());
        }
    }

    public static void dangKyClient(
            String clientId,
            String username,
            String tenMay,
            String heDieuHanh) {

        String sql = """
            INSERT INTO clients
                (client_id, username, computer_name,
                 operating_system, last_seen, status)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, 'Online')
            ON CONFLICT(client_id) DO UPDATE SET
                username = excluded.username,
                computer_name = excluded.computer_name,
                operating_system = excluded.operating_system,
                last_seen = CURRENT_TIMESTAMP,
                status = 'Online'
        """;

        try (Connection ketNoi = ketNoi();
             PreparedStatement lenh = ketNoi.prepareStatement(sql)) {

            lenh.setString(1, clientId);
            lenh.setString(2, username);
            lenh.setString(3, tenMay);
            lenh.setString(4, heDieuHanh);
            lenh.executeUpdate();

            ghiLichSu(clientId, "CONNECT", "Client ket noi Server");

        } catch (SQLException e) {
            System.out.println("Loi dang ky Client: " + e.getMessage());
        }
    }

    public static void ghiLichSu(
            String clientId,
            String loaiSuKien,
            String noiDung) {

        String sql = """
            INSERT INTO activity_history
                (client_id, event_type, description)
            VALUES (?, ?, ?)
        """;

        try (Connection ketNoi = ketNoi();
             PreparedStatement lenh = ketNoi.prepareStatement(sql)) {

            lenh.setString(1, clientId);
            lenh.setString(2, loaiSuKien);
            lenh.setString(3, noiDung);
            lenh.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Loi ghi History: " + e.getMessage());
        }
    }

    public static void capNhatOffline(String clientId) {
        String sql = """
            UPDATE clients
            SET status = 'Offline',
                last_seen = CURRENT_TIMESTAMP
            WHERE client_id = ?
        """;

        try (Connection ketNoi = ketNoi();
             PreparedStatement lenh = ketNoi.prepareStatement(sql)) {

            lenh.setString(1, clientId);
            int soDong = lenh.executeUpdate();

            if (soDong > 0) {
                System.out.println("Da cap nhat Offline: " + clientId);
                ghiLichSu(clientId, "DISCONNECT",
                        "Client ngat ket noi");
            } else {
                System.out.println(
                        "Khong tim thay Client: " + clientId);
            }

        } catch (SQLException e) {
            System.out.println("Loi cap nhat Offline: " + e.getMessage());
        }
    }

    // Lay tat ca Client, bao gom ca Online va Offline
    public static List<ThongTinClient> layDanhSachClient() {
        List<ThongTinClient> danhSach = new ArrayList<>();

        String sql = """
            SELECT client_id, username, computer_name,
                   operating_system, registered_at, last_seen, status
            FROM clients
            ORDER BY username COLLATE NOCASE
        """;

        try (Connection ketNoi = ketNoi();
             PreparedStatement lenh = ketNoi.prepareStatement(sql);
             ResultSet ketQua = lenh.executeQuery()) {

            while (ketQua.next()) {
                danhSach.add(new ThongTinClient(
                        ketQua.getString("client_id"),
                        ketQua.getString("username"),
                        ketQua.getString("computer_name"),
                        ketQua.getString("operating_system"),
                        ketQua.getString("registered_at"),
                        ketQua.getString("last_seen"),
                        ketQua.getString("status")
                ));
            }

        } catch (SQLException e) {
            System.out.println("Loi lay danh sach Client: "
                    + e.getMessage());
        }

        return danhSach;
    }

    public static void xemLichSu(String clientId) {
        String sql = """
            SELECT event_type, description, created_at
            FROM activity_history
            WHERE client_id = ?
            ORDER BY history_id DESC
        """;

        try (Connection ketNoi = ketNoi();
             PreparedStatement lenh = ketNoi.prepareStatement(sql)) {

            lenh.setString(1, clientId);

            try (ResultSet ketQua = lenh.executeQuery()) {
                while (ketQua.next()) {
                    System.out.println(
                            ketQua.getString("created_at") + " | "
                                    + ketQua.getString("event_type") + " | "
                                    + ketQua.getString("description")
                    );
                }
            }

        } catch (SQLException e) {
            System.out.println("Loi doc History: " + e.getMessage());
        }
    }

    public static void xoaClient(String clientId) {
        String sql = "DELETE FROM clients WHERE client_id = ?";

        try (Connection ketNoi = ketNoi();
             PreparedStatement lenh = ketNoi.prepareStatement(sql)) {

            lenh.setString(1, clientId);
            int soDong = lenh.executeUpdate();

            if (soDong > 0) {
                System.out.println("Da xoa Client: " + clientId);
            } else {
                System.out.println("Khong tim thay Client.");
            }

        } catch (SQLException e) {
            System.out.println("Loi xoa Client: " + e.getMessage());
        }
    }
}
