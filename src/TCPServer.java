import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class TCPServer {
    public static final int CHAT_PORT = 5000;
    public static final int FILE_PORT = 5001;
    public static final int MAX_CLIENT = 10;

    private ServerSocket serverChat;
    private ServerSocket serverFile;
    private boolean running = false;
    private final ArrayList<ClientHandler> listClient = new ArrayList<>();
    private final ServerListener listener;

    public interface ServerListener {
        void onServerStatus(boolean isOnline);

        void onClientListChanged();

        void onLog(String log);
    }

    public TCPServer(ServerListener listener) {
        this.listener = listener;
    }

    public void startServer() throws Exception {
        if (running) return;
        DatabaseManager.khoiTaoDatabase();
        serverChat = new ServerSocket(CHAT_PORT);
        serverFile = new ServerSocket(FILE_PORT);
        running = true;

        new Thread(() -> {
            while (running) {
                try {
                    Socket socket = serverChat.accept();
                    if (listClient.size() >= MAX_CLIENT) {
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                        out.writeUTF("DISCONNECT Server da day.");
                        socket.close();
                        continue;
                    }

                    ClientHandler handler = new ClientHandler(socket);
                    synchronized (listClient) {
                        listClient.add(handler);
                    }
                    handler.start();

                } catch (IOException e) {
                    if (!running) break;
                }
            }
        }).start();

        new Thread(() -> {
            while (running) {
                try {
                    Socket fileSocket = serverFile.accept();
                    chuyenTiepFile(fileSocket);
                } catch (IOException e) {
                    if (!running) break;
                }
            }
        }).start();

        if (listener != null) listener.onServerStatus(true);
    }

    private void chuyenTiepFile(Socket socket) {
        new Thread(() -> {
            try {
                DataInputStream in = new DataInputStream(socket.getInputStream());
                String nguoiNhan = in.readUTF();
                String nguoiGui = in.readUTF();
                String tenFile = in.readUTF();
                long doDai = in.readLong();

                ClientHandler target = getClientByName(nguoiNhan);
                if (target != null) {
                    target.gui("FILE_INCOMING " + nguoiGui + " da gui file: " + tenFile);

                    Socket sendSock = new Socket(target.getIp(), 5002);
                    DataOutputStream outTarget = new DataOutputStream(sendSock.getOutputStream());
                    outTarget.writeUTF(tenFile);
                    outTarget.writeLong(doDai);

                    byte[] buffer = new byte[4096];
                    int byteDoc;
                    long daChuyen = 0;
                    while (daChuyen < doDai && (byteDoc = in.read(buffer, 0, (int) Math.min(buffer.length, doDai - daChuyen))) != -1) {
                        outTarget.write(buffer, 0, byteDoc);
                        daChuyen += byteDoc;
                    }
                    outTarget.flush();
                    sendSock.close();


                    if (listener != null) {
                        listener.onLog(
                                "Chuyen file " + tenFile + " tu " + nguoiGui + " sang " + nguoiNhan
                        );
                    }

// Luu lich su gui file vao database
                    ClientHandler sender = getClientByName(nguoiGui);

                    if (sender != null && sender.daDangKyDatabase()) {
                        DatabaseManager.ghiLichSu(
                                sender.getClientId(),
                                "SEND_FILE",
                                "Gui file " + tenFile + " cho " + nguoiNhan
                        );
                    }

                    ClientHandler receiver = getClientByName(nguoiNhan);

                    if (receiver != null && receiver.daDangKyDatabase()) {
                        DatabaseManager.ghiLichSu(
                                receiver.getClientId(),
                                "RECEIVE_FILE",
                                "Nhan file " + tenFile + " tu " + nguoiGui
                        );
                    }
                }
                socket.close();
            } catch (Exception e) {
                if (listener != null) listener.onLog("Loi chuyen file: " + e.getMessage());
            }
        }).start();
    }


    public void stopServer() {
        if (!running) return;

        running = false;

        synchronized (listClient) {
            for (ClientHandler c : listClient) {
                c.gui("DISCONNECT Server da dung hoat dong.");

                if (c.daDangKyDatabase()) {
                    DatabaseManager.capNhatOffline(c.getClientId());
                }

                c.dongKetNoi();
            }

            listClient.clear();
        }

        try {
            if (serverChat != null) serverChat.close();
        } catch (Exception ignored) {
        }

        try {
            if (serverFile != null) serverFile.close();
        } catch (Exception ignored) {
        }

        if (listener != null) {
            listener.onClientListChanged();
            listener.onServerStatus(false);
        }
    }


    public void guiTatCa(String noiDung) {
        synchronized (listClient) {
            for (ClientHandler c : listClient) {
                c.gui("MSG " + noiDung);
            }
        }
    }

    // Server nhắn tin riêng tới 1 Client theo username
    public void serverGuiTinRieng(String username, String noiDung) {
        ClientHandler target = getClientByName(username);
        if (target != null) {
            target.gui("MSG [SERVER -> BAN (Rieng)]: " + noiDung);
        }
    }

    // Kick client dứt điểm
    public void kickClient(int index) {
        synchronized (listClient) {
            if (index >= 0 && index < listClient.size()) {
                ClientHandler c = listClient.get(index);
                c.gui("DISCONNECT Ban da bi quan tri vien ngat ket noi.");
                c.dongKetNoi();
                xoaClient(c);
            }
        }
    }

    public void broadcastDanhSachUser() {
        StringBuilder sb = new StringBuilder("USERS ");
        synchronized (listClient) {
            for (ClientHandler c : listClient) {
                sb.append(c.getUsername()).append(",");
            }
            for (ClientHandler c : listClient) {
                c.gui(sb.toString());
            }
        }
    }


    public void xoaClient(ClientHandler client) {
        boolean removed;

        synchronized (listClient) {
            removed = listClient.remove(client);
        }

        // Chi xu ly khi Client thuc su duoc xoa khoi danh sach Online
        if (removed) {
            if (client.daDangKyDatabase()) {
                DatabaseManager.capNhatOffline(client.getClientId());
            }

            guiTatCa("[HE THONG] " + client.getUsername() + " da roi mang.");

            if (listener != null) {
                listener.onLog("Client " + client.getUsername() + " da ngat ket noi.");
                listener.onClientListChanged();
            }

            broadcastDanhSachUser();
        }
    }


    public ClientHandler getClientByName(String name) {
        synchronized (listClient) {
            for (ClientHandler c : listClient) {
                if (c.getUsername().equalsIgnoreCase(name)) return c;
            }
        }
        return null;
    }

    public ArrayList<ClientHandler> getListClient() {
        return listClient;
    }

    public boolean isRunning() {
        return running;
    }

    // ==========================================
    // CLIENT HANDLER
    // ==========================================
    public class ClientHandler extends Thread {
        private final Socket socket;
        private DataInputStream in;
        private DataOutputStream out;
        private volatile boolean isConnected = true;

        private String clientId = "";
        private String username = "Unknown";
        private String osName = "Unknown";
        private boolean daDangKyDatabase = false;
        private final long thoiDiemVao;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.thoiDiemVao = System.currentTimeMillis();
            try {
                this.in = new DataInputStream(socket.getInputStream());
                this.out = new DataOutputStream(socket.getOutputStream());
            } catch (IOException ignored) {
            }
        }

        public String getUsername() {
            return username;
        }

        public String getOsName() {
            return osName;
        }

        public String getIp() {
            return socket.getInetAddress().getHostAddress();
        }

        public int getPort() {
            return socket.getPort();
        }

        public String getClientId() {
            return clientId;
        }

        public boolean daDangKyDatabase() {
            return daDangKyDatabase;
        }

        public String getUptime() {
            long giay = (System.currentTimeMillis() - thoiDiemVao) / 1000;
            return String.format("%02d:%02d:%02d", giay / 3600, (giay % 3600) / 60, giay % 60);
        }


        @Override
        public void run() {
            try {
                while (isConnected) {
                    String line = in.readUTF();

                    if (line.startsWith("LOGIN ")) {
                        String[] phan = line.substring(6).split("\\|", -1);

                        if (phan.length >= 3) {
                            // Giao thuc moi: LOGIN clientId|username|OS
                            this.clientId = phan[0];
                            this.username = phan[1];
                            this.osName = phan[2];
                        } else {
                            // Giao thuc cu: LOGIN username|OS
                            this.username = phan[0];

                            if (phan.length > 1) {
                                this.osName = phan[1];
                            }

                            this.clientId = "NAME:" + this.username;
                        }

                        // Luu thong tin Client vao database
                        DatabaseManager.dangKyClient(
                                clientId,
                                username,
                                getIp(),
                                osName
                        );

                        daDangKyDatabase = true;

                        guiTatCa(
                                "[HE THONG] " + username + " da tham gia mang."
                        );

                        if (listener != null) {
                            listener.onLog(
                                    "Client " + username + " ket noi thanh cong."
                            );
                            listener.onClientListChanged();
                        }

                        broadcastDanhSachUser();

                    } else if (line.startsWith("CHAT_ALL ")) {
                        String noiDung = line.substring(9);

                        // Gui tin nhan den tat ca Client
                        guiTatCa("[" + username + "]: " + noiDung);

                        // Luu lich su tin nhan gui chung
                        if (daDangKyDatabase) {
                            DatabaseManager.ghiLichSu(
                                    clientId,
                                    "CHAT_ALL",
                                    noiDung
                            );
                        }

                    } else if (line.startsWith("CHAT_ONE ")) {
                        String[] phan = line.substring(9).split("\\|", 2);

                        if (phan.length == 2) {
                            ClientHandler target = getClientByName(phan[0]);

                            if (target != null) {
                                // Gui tin nhan den Client nhan
                                target.gui(
                                        "[" + username + " -> BAN]: " + phan[1]
                                );

                                // Luu lich su cua nguoi gui
                                if (daDangKyDatabase) {
                                    DatabaseManager.ghiLichSu(
                                            clientId,
                                            "CHAT_ONE",
                                            "Gui cho " + target.getUsername()
                                                    + ": " + phan[1]
                                    );
                                }

                                // Luu lich su cua nguoi nhan
                                if (target.daDangKyDatabase()) {
                                    DatabaseManager.ghiLichSu(
                                            target.getClientId(),
                                            "CHAT_ONE",
                                            "Nhan tu " + username
                                                    + ": " + phan[1]
                                    );
                                }
                            }
                        }

                    } else if ("EXIT".equalsIgnoreCase(line)) {
                        break;
                    }
                }

            } catch (IOException ignored) {
            } finally {
                dongKetNoi();
                xoaClient(this);
            }
        }


        public synchronized void gui(String msg) {
            if (!isConnected) return;
            try {
                out.writeUTF(msg);
                out.flush();
            } catch (IOException e) {
                dongKetNoi();
            }
        }

        public void dongKetNoi() {
            isConnected = false;
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ignored) {}
        }
    }
}