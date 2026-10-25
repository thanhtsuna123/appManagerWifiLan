
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ServerGUI extends JFrame implements TCPServer.ServerListener {

    private final TCPServer server;

    private JLabel lblStatus;
    private JLabel lblClients;

    private DefaultTableModel modelClient;
    private JTable bangClient;

    private DefaultTableModel modelThongBao;
    private JTable bangThongBao;

    private JTextArea txtLogs;

    private JButton btnStart;
    private JButton btnStop;
    private JButton btnHistory;
    private JButton btnChatRieng;
    private JButton btnThongBao;
    private JButton btnKick;
    private JButton btnLamMoi;

    private boolean serverOnline = false;

    private final DateTimeFormatter dinhDang =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public ServerGUI() {
        server = new TCPServer(this);

        khoiTaoGiaoDien();

        setTitle("QUAN LY KET NOI MANG LAN");
        setSize(1450, 780);
        setMinimumSize(new Dimension(1050, 650));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Tu dong cap nhat danh sach Client tu SQLite
        Timer timer = new Timer(2000, e -> {
            taiDanhSachClient();
            capNhatSoClientOnline();
        });
        timer.start();
    }

    private void khoiTaoGiaoDien() {
        JPanel panelChinh = new JPanel(new BorderLayout(8, 8));
        panelChinh.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );

        // ================= PHAN DAU =================
        JPanel panelDau = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 18, 5)
        );

        JLabel lblTieuDe = new JLabel(
                "QUAN LY KET NOI MANG LAN - SERVER"
        );
        lblTieuDe.setFont(new Font("Arial", Font.BOLD, 19));

        lblStatus = new JLabel("SERVER: OFFLINE");
        lblStatus.setForeground(Color.RED);
        lblStatus.setFont(new Font("Arial", Font.BOLD, 14));

        lblClients = new JLabel("ONLINE: 0");
        lblClients.setFont(new Font("Arial", Font.BOLD, 14));

        panelDau.add(lblTieuDe);
        panelDau.add(lblStatus);
        panelDau.add(lblClients);

        panelChinh.add(panelDau, BorderLayout.NORTH);

        // ================= BANG CLIENT =================
        String[] cotClient = {
                "CLIENT ID",
                "TEN CLIENT",
                "TEN MAY / IP",
                "HE DIEU HANH",
                "NGAY DANG KY",
                "LAN CUOI",
                "TRANG THAI"
        };

        modelClient = new DefaultTableModel(cotClient, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        bangClient = new JTable(modelClient);
        bangClient.setRowHeight(25);
        bangClient.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        bangClient.setAutoCreateRowSorter(true);

        // An cot ID, nhung van giu ID de tim dung Client
        bangClient.removeColumn(
                bangClient.getColumnModel().getColumn(0)
        );

        JScrollPane scrollClient = new JScrollPane(bangClient);
        scrollClient.setBorder(
                BorderFactory.createTitledBorder(
                        "DANH SACH CLIENT ONLINE / OFFLINE"
                )
        );

        // ================= BANG THONG BAO =================
        String[] cotThongBao = {
                "THOI GIAN",
                "LOAI THONG BAO",
                "NOI DUNG"
        };

        modelThongBao = new DefaultTableModel(cotThongBao, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        bangThongBao = new JTable(modelThongBao);
        bangThongBao.setRowHeight(24);
        bangThongBao.setAutoResizeMode(
                JTable.AUTO_RESIZE_LAST_COLUMN
        );

        JScrollPane scrollThongBao = new JScrollPane(bangThongBao);
        scrollThongBao.setBorder(
                BorderFactory.createTitledBorder(
                        "THONG TIN CHUNG"
                )
        );

        JSplitPane splitBang = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                scrollClient,
                scrollThongBao
        );
        splitBang.setResizeWeight(0.60);

        // ================= NHAT KY SERVER =================
        txtLogs = new JTextArea();
        txtLogs.setEditable(false);
        txtLogs.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtLogs.setLineWrap(true);
        txtLogs.setWrapStyleWord(true);

        JScrollPane scrollLog = new JScrollPane(txtLogs);
        scrollLog.setBorder(
                BorderFactory.createTitledBorder(
                        "NHAT KY HOAT DONG SERVER"
                )
        );

        JSplitPane splitChinh = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                splitBang,
                scrollLog
        );
        splitChinh.setResizeWeight(0.72);

        panelChinh.add(splitChinh, BorderLayout.CENTER);

        // ================= CAC NUT =================
        JPanel panelNut = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 8, 5)
        );

        btnStart = new JButton("Khoi dong Server");
        btnStop = new JButton("Dung Server");
        btnHistory = new JButton("Xem History");
        btnChatRieng = new JButton("Chat rieng");
        btnThongBao = new JButton("Gui thong bao chung");
        btnKick = new JButton("Ngat ket noi Client");
        btnLamMoi = new JButton("Lam moi danh sach");

        btnStart.addActionListener(e -> {
            try {
                server.startServer();
            } catch (Exception ex) {
                ghiLog("Loi khoi dong Server: " + ex.getMessage());
                themThongBao("LOI SERVER", ex.getMessage());

                JOptionPane.showMessageDialog(
                        this,
                        "Khong the khoi dong Server:\n"
                                + ex.getMessage()
                );
            }
        });

        btnStop.addActionListener(e -> server.stopServer());

        btnLamMoi.addActionListener(e -> taiDanhSachClient());

        btnHistory.addActionListener(e -> moHistoryClient());

        btnChatRieng.addActionListener(e -> guiChatRieng());

        btnThongBao.addActionListener(e -> guiThongBaoChung());

        btnKick.addActionListener(e -> ngatClient());

        panelNut.add(btnStart);
        panelNut.add(btnStop);
        panelNut.add(btnHistory);
        panelNut.add(btnChatRieng);
        panelNut.add(btnThongBao);
        panelNut.add(btnKick);
        panelNut.add(btnLamMoi);

        panelChinh.add(panelNut, BorderLayout.SOUTH);
        setContentPane(panelChinh);

        capNhatNut();
    }

    // ================= DOC CLIENT TU DATABASE =================
    private void taiDanhSachClient() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::taiDanhSachClient);
            return;
        }

        int dongDangChon = bangClient.getSelectedRow();
        String idDangChon = null;

        if (dongDangChon >= 0) {
            int dongModel = bangClient.convertRowIndexToModel(dongDangChon);
            idDangChon = String.valueOf(
                    modelClient.getValueAt(dongModel, 0)
            );
        }

        try {
            List<DatabaseManager.ThongTinClient> danhSach =
                    DatabaseManager.layDanhSachClient();

            modelClient.setRowCount(0);

            for (DatabaseManager.ThongTinClient c : danhSach) {
                String trangThai = c.trangThai == null
                        ? "Offline" : c.trangThai;

                modelClient.addRow(new Object[]{
                        c.clientId,
                        c.username,
                        c.tenMay,
                        c.heDieuHanh,
                        c.ngayDangKy,
                        c.lanCuoi,
                        trangThai
                });
            }

            // Chon lai Client cu neu van con trong danh sach
            if (idDangChon != null) {
                for (int i = 0; i < modelClient.getRowCount(); i++) {
                    if (idDangChon.equals(
                            String.valueOf(modelClient.getValueAt(i, 0)))) {

                        int viewRow = bangClient.convertRowIndexToView(i);
                        if (viewRow >= 0) {
                            bangClient.setRowSelectionInterval(
                                    viewRow, viewRow
                            );
                        }
                        break;
                    }
                }
            }

            capNhatSoClientOnline();

        } catch (Exception ex) {
            ghiLog("Loi doc danh sach Client: " + ex.getMessage());
        }
    }

    // ================= DEM CLIENT ONLINE =================
    private void capNhatSoClientOnline() {
        int online = 0;

        for (int i = 0; i < modelClient.getRowCount(); i++) {
            Object trangThai = modelClient.getValueAt(i, 6);

            if (trangThai != null
                    && trangThai.toString().equalsIgnoreCase("Online")) {
                online++;
            }
        }

        lblClients.setText("ONLINE: " + online);
    }

    // ================= LAY CLIENT DUOC CHON =================
    private String layGiaTriCotDangChon(int cotModel) {
        int viewRow = bangClient.getSelectedRow();

        if (viewRow < 0) {
            JOptionPane.showMessageDialog(
                    this, "Vui long chon mot Client!"
            );
            return null;
        }

        int modelRow = bangClient.convertRowIndexToModel(viewRow);
        Object value = modelClient.getValueAt(modelRow, cotModel);

        return value == null ? "" : value.toString();
    }

    // ================= MO LICH SU CLIENT =================
    private void moHistoryClient() {
        String clientId = layGiaTriCotDangChon(0);
        if (clientId == null) return;

        String username = layGiaTriCotDangChon(1);
        if (username == null) return;

        try {
            ClientHistoryGUI history =
                    new ClientHistoryGUI(clientId, username);
            history.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Khong mo duoc History:\n" + ex.getMessage()
            );
        }
    }

    // ================= TIM CLIENT DANG ONLINE =================
    private TCPServer.ClientHandler timClientOnline(String clientId) {
        ArrayList<TCPServer.ClientHandler> danhSach =
                server.getListClient();

        synchronized (danhSach) {
            for (TCPServer.ClientHandler c : danhSach) {
                if (clientId.equals(c.getClientId())) {
                    return c;
                }
            }
        }

        return null;
    }

    private int timViTriClientOnline(String clientId) {
        ArrayList<TCPServer.ClientHandler> danhSach =
                server.getListClient();

        synchronized (danhSach) {
            for (int i = 0; i < danhSach.size(); i++) {
                if (clientId.equals(danhSach.get(i).getClientId())) {
                    return i;
                }
            }
        }

        return -1;
    }

    // ================= CHAT RIENG =================
    private void guiChatRieng() {
        String clientId = layGiaTriCotDangChon(0);
        if (clientId == null) return;

        String username = layGiaTriCotDangChon(1);
        if (username == null) return;

        TCPServer.ClientHandler client = timClientOnline(clientId);

        if (client == null) {
            JOptionPane.showMessageDialog(
                    this, "Client nay dang Offline!"
            );
            return;
        }

        String tinNhan = JOptionPane.showInputDialog(
                this, "Nhap tin nhan gui den " + username + ":"
        );

        if (tinNhan != null && !tinNhan.trim().isEmpty()) {
            server.serverGuiTinRieng(username, tinNhan);

            String noiDung = "Server gui tin rieng den "
                    + username + ": " + tinNhan;

            ghiLog(noiDung);
            themThongBao("CHAT RIENG", noiDung);
        }
    }

    // ================= GUI THONG BAO CHUNG =================
    private void guiThongBaoChung() {
        String noiDung = JOptionPane.showInputDialog(
                this, "Nhap thong bao gui den cac Client:"
        );

        if (noiDung != null && !noiDung.trim().isEmpty()) {
            String thongBao = "[THONG BAO SERVER] " + noiDung;

            server.guiTatCa(thongBao);

            ghiLog(thongBao);
            themThongBao("THONG BAO CHUNG", noiDung);
        }
    }

    // ================= NGAT CLIENT =================
    private void ngatClient() {
        String clientId = layGiaTriCotDangChon(0);
        if (clientId == null) return;

        String username = layGiaTriCotDangChon(1);
        if (username == null) return;

        int viTri = timViTriClientOnline(clientId);

        if (viTri < 0) {
            JOptionPane.showMessageDialog(
                    this, "Client nay da Offline, khong the ngat ket noi!"
            );
            return;
        }

        int xacNhan = JOptionPane.showConfirmDialog(
                this,
                "Ban co muon ngat ket noi Client " + username + "?",
                "Xac nhan",
                JOptionPane.YES_NO_OPTION
        );

        if (xacNhan == JOptionPane.YES_OPTION) {
            server.kickClient(viTri);
        }
    }

    // ================= THEM THONG BAO =================
    private void themThongBao(String loai, String noiDung) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(
                    () -> themThongBao(loai, noiDung)
            );
            return;
        }

        String thoiGian = LocalDateTime.now().format(dinhDang);

        modelThongBao.addRow(new Object[]{
                thoiGian, loai, noiDung
        });

        // Giu toi da 500 thong bao trong giao dien
        while (modelThongBao.getRowCount() > 500) {
            modelThongBao.removeRow(0);
        }

        int dongCuoi = modelThongBao.getRowCount() - 1;

        if (dongCuoi >= 0) {
            bangThongBao.scrollRectToVisible(
                    bangThongBao.getCellRect(dongCuoi, 0, true)
            );
        }
    }

    // ================= GHI NHAT KY =================
    private void ghiLog(String noiDung) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> ghiLog(noiDung));
            return;
        }

        String thoiGian = LocalDateTime.now().format(dinhDang);

        txtLogs.append(
                "[" + thoiGian + "] " + noiDung + "\n"
        );

        txtLogs.setCaretPosition(
                txtLogs.getDocument().getLength()
        );
    }

    private void capNhatNut() {
        btnStart.setEnabled(!serverOnline);
        btnStop.setEnabled(serverOnline);
        btnThongBao.setEnabled(serverOnline);
        btnChatRieng.setEnabled(serverOnline);
        btnKick.setEnabled(serverOnline);
    }

    // ================= CALLBACK TU TCPSERVER =================
    @Override
    public void onClientListChanged() {
        SwingUtilities.invokeLater(() -> {
            taiDanhSachClient();
            themThongBao(
                    "DANH SACH CLIENT",
                    "Danh sach Client da duoc cap nhat."
            );
        });
    }

    @Override
    public void onServerStatus(boolean isOnline) {
        SwingUtilities.invokeLater(() -> {
            serverOnline = isOnline;

            lblStatus.setText(
                    isOnline ? "SERVER: ONLINE" : "SERVER: OFFLINE"
            );

            lblStatus.setForeground(
                    isOnline ? new Color(0, 130, 50) : Color.RED
            );

            capNhatNut();

            String noiDung = isOnline
                    ? "Server da khoi dong."
                    : "Server da dung.";

            ghiLog(noiDung);
            themThongBao("TRANG THAI SERVER", noiDung);

            taiDanhSachClient();
        });
    }

    @Override
    public void onLog(String log) {
        SwingUtilities.invokeLater(() -> {
            ghiLog(log);

            String chuThuong = log.toLowerCase();
            String loai = "HE THONG";

            if (chuThuong.contains("connect")
                    || chuThuong.contains("ket noi")
                    || chuThuong.contains("dang nhap")) {
                loai = "KET NOI CLIENT";
            } else if (chuThuong.contains("disconnect")
                    || chuThuong.contains("ngat")
                    || chuThuong.contains("roi mang")) {
                loai = "NGAT KET NOI";
            } else if (chuThuong.contains("file")) {
                loai = "TRUYEN FILE";
            } else if (chuThuong.contains("loi")
                    || chuThuong.contains("error")) {
                loai = "LOI";
            }

            themThongBao(loai, log);
            taiDanhSachClient();
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerGUI gui = new ServerGUI();
            gui.setVisible(true);
            gui.taiDanhSachClient();
        });
    }
}
