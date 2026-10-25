import javax.swing.*;
import java.awt.*;
import java.io.File;

public class ClientsGUI extends JFrame implements TCPClients.ClientListener {

    private final TCPClients client;
    private String myUsername;
    private String serverIp;

    private JTextArea txtChat;
    private JTextField txtTinNhan;
    private DefaultListModel<String> userModel;
    private JList<String> listUsers;
    private JComboBox<String> cbTarget;

    private JButton btnKetNoi, btnGui, btnGuiFile;

    public ClientsGUI() {
        client = new TCPClients(this);
        khoiTaoGiaoDien();

        setTitle("NGƯỜI DÙNG");
        setSize(750, 480);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void khoiTaoGiaoDien() {
        JPanel panelChinh = new JPanel(new BorderLayout(8, 8));
        panelChinh.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // HEADER
        JPanel panelTren = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JTextField txtIp = new JTextField("127.0.0.1", 10);
        JTextField txtUser = new JTextField("May_" + (int)(Math.random() * 100), 8);
        btnKetNoi = new JButton("Ket noi");

        btnKetNoi.addActionListener(e -> {
            try {
                this.serverIp = txtIp.getText().trim();
                this.myUsername = txtUser.getText().trim();
                client.ketNoi(serverIp, TCPServer.CHAT_PORT, myUsername);
                btnKetNoi.setEnabled(false);
                txtIp.setEnabled(false);
                txtUser.setEnabled(false);
                txtChat.append("[HE THONG] Da ket noi voi Server!\n");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ket noi that bai: " + ex.getMessage());
            }
        });

        panelTren.add(new JLabel("Server IP:"));
        panelTren.add(txtIp);
        panelTren.add(new JLabel("User:"));
        panelTren.add(txtUser);
        panelTren.add(btnKetNoi);
        panelChinh.add(panelTren, BorderLayout.NORTH);

        // KHUNG CHAT
        txtChat = new JTextArea();
        txtChat.setEditable(false);
        panelChinh.add(new JScrollPane(txtChat), BorderLayout.CENTER);

        // DANH SACH ONLINE
        JPanel panelPhai = new JPanel(new BorderLayout(5, 5));
        panelPhai.setPreferredSize(new Dimension(160, 0));
        panelPhai.add(new JLabel("DANH SACH ONLINE", SwingConstants.CENTER), BorderLayout.NORTH);

        userModel = new DefaultListModel<>();
        listUsers = new JList<>(userModel);
        panelPhai.add(new JScrollPane(listUsers), BorderLayout.CENTER);
        panelChinh.add(panelPhai, BorderLayout.EAST);

        // KHU VUC SOAN TIN NHAN VA GUI FILE
        JPanel panelDuoi = new JPanel(new BorderLayout(5, 5));
        cbTarget = new JComboBox<>(new String[]{"Tat ca (Broadcast)"});

        txtTinNhan = new JTextField();
        txtTinNhan.addActionListener(e -> guiTinNhan());

        btnGui = new JButton("Gui");
        btnGui.addActionListener(e -> guiTinNhan());

        btnGuiFile = new JButton("Gui File");
        btnGuiFile.addActionListener(e -> {
            String nguoiNhan = (String) cbTarget.getSelectedItem();
            if ("Tat ca (Broadcast)".equals(nguoiNhan)) {
                JOptionPane.showMessageDialog(this, "Hay chon 1 nguoi dung cu the trong danh sach de gui file.");
                return;
            }
            JFileChooser chonFile = new JFileChooser();
            if (chonFile.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = chonFile.getSelectedFile();
                client.guiFile(serverIp, myUsername, nguoiNhan, f);
            }
        });

        JPanel pnlNut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        pnlNut.add(cbTarget);
        pnlNut.add(btnGuiFile);
        pnlNut.add(btnGui);

        panelDuoi.add(txtTinNhan, BorderLayout.CENTER);
        panelDuoi.add(pnlNut, BorderLayout.EAST);
        panelChinh.add(panelDuoi, BorderLayout.SOUTH);

        setContentPane(panelChinh);
    }

    private void guiTinNhan() {
        String msg = txtTinNhan.getText().trim();
        if (msg.isEmpty()) return;

        String target = (String) cbTarget.getSelectedItem();
        if ("Tat ca (Broadcast)".equals(target)) {
            client.guiChatTatCa(msg);
        } else {
            client.guiChatRieng(target, msg);
            txtChat.append("[BAN -> " + target + "]: " + msg + "\n");
        }
        txtTinNhan.setText("");
    }

    @Override
    public void onMessage(String msg) {
        SwingUtilities.invokeLater(() -> txtChat.append(msg + "\n"));
    }

    @Override
    public void onUserList(String[] users) {
        SwingUtilities.invokeLater(() -> {
            userModel.clear();
            cbTarget.removeAllItems();
            cbTarget.addItem("Tat ca (Broadcast)");

            for (String u : users) {
                if (!u.trim().isEmpty()) {
                    userModel.addElement(u);
                    if (!u.equalsIgnoreCase(myUsername)) {
                        cbTarget.addItem(u);
                    }
                }
            }
        });
    }

    @Override
    public void onFileReceived(String tenFile) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Ban da nhan duoc file: " + tenFile + "\nLuu tai thu muc Download."));
    }

    @Override
    public void onDisconnect(String lyDo) {
        SwingUtilities.invokeLater(() -> {
            txtChat.append("[NGAT KET NOI] " + lyDo + "\n");
            btnKetNoi.setEnabled(true);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientsGUI().setVisible(true));
    }
}