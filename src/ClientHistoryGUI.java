
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClientHistoryGUI extends JFrame {

    private final String clientId;
    private final String username;

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Thoi gian", "Loai su kien", "Noi dung"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable bangLichSu = new JTable(model);

    public ClientHistoryGUI(String clientId, String username) {
        this.clientId = clientId;
        this.username = username;

        setTitle("Lich su Client - " + username);
        setSize(800, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JLabel tieuDe = new JLabel(
                "Lich su hoat dong cua Client: " + username
        );
        tieuDe.setFont(new Font("Arial", Font.BOLD, 18));

        JLabel maClient = new JLabel("Client ID: " + clientId);

        JButton nutLamMoi = new JButton("Lam moi");
        nutLamMoi.addActionListener(e -> taiLichSu());

        JPanel phanDau = new JPanel(new BorderLayout(5, 5));
        JPanel thongTin = new JPanel(new GridLayout(2, 1, 5, 5));

        thongTin.add(tieuDe);
        thongTin.add(maClient);

        phanDau.add(thongTin, BorderLayout.CENTER);
        phanDau.add(nutLamMoi, BorderLayout.EAST);
        phanDau.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );

        bangLichSu.setRowHeight(25);
        bangLichSu.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane cuon = new JScrollPane(bangLichSu);

        setLayout(new BorderLayout());
        add(phanDau, BorderLayout.NORTH);
        add(cuon, BorderLayout.CENTER);

        taiLichSu();
    }

    private void taiLichSu() {
        model.setRowCount(0);

        String sql = """
                SELECT created_at, event_type, description
                FROM activity_history
                WHERE client_id = ?
                ORDER BY history_id DESC
                """;

        try (Connection ketNoi = DatabaseManager.ketNoi();
             PreparedStatement lenh = ketNoi.prepareStatement(sql)) {

            lenh.setString(1, clientId);

            try (ResultSet ketQua = lenh.executeQuery()) {
                while (ketQua.next()) {
                    model.addRow(new Object[]{
                            ketQua.getString("created_at"),
                            ketQua.getString("event_type"),
                            ketQua.getString("description")
                    });
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Loi doc lich su: " + e.getMessage(),
                    "Loi Database",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
