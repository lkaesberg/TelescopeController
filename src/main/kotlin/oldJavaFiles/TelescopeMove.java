package oldJavaFiles;

import com.fazecast.jSerialComm.SerialPort;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;

public class TelescopeMove implements ActionListener, KeyListener {
    JFrame window = new JFrame("Telescope - Move");
    JButton connectB = new JButton("Connect");
    JButton moveB = new JButton("M");
    JButton centerB = new JButton("Center");
    JButton rotateB = new JButton("Follow");
    JLabel offsetL = new JLabel("Offset: X: 0 Y: 0");
    JLabel posL = new JLabel("");
    JLabel rotateL = new JLabel("<html>Follow: false<br/>Name:<br/>Typ:</html>");
    JLabel targetPosL = new JLabel("Target: X: 0 Y: 0");
    JTextArea textSpeed = new JTextArea("400");
    int posX, posY, offsetX, offsetY = 0;
    int speedMot = 400;
    float radius, speed, angle = 0;
    PrintWriter out;
    BufferedReader in;
    SerialPort arduino = null;
    boolean connected = false;
    boolean rotate = false;

    public TelescopeMove() {
        connectB.addActionListener(this);
        connectB.setBounds(50, 30, 200, 50);

        moveB.setBounds(50, 30, 50, 50);
        moveB.addKeyListener(this);

        centerB.setBounds(300, 30, 75, 50);
        centerB.addActionListener(this);

        rotateB.setBounds(50, 90, 75, 50);
        rotateB.addActionListener(this);

        offsetL.setBounds(110, 25, 200, 25);

        posL.setBounds(110, 0, 200, 25);
        targetPosL.setBounds(110, 50, 200, 25);

        rotateL.setBounds(185, 90, 200, 100);
        rotateL.setVerticalAlignment(JLabel.TOP);

        textSpeed.setBounds(300, 100, 50, 25);
        textSpeed.addKeyListener(this);

        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);
        window.setResizable(false);
        window.setLayout(null);
        window.setSize(400, 200);
        window.setAlwaysOnTop(true);
        window.add(connectB);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        long time = System.currentTimeMillis();
        TelescopeMove main = new TelescopeMove();
        while (true) {
            Thread.sleep(100);
            if (main.in != null) {
                int del = 0;
                if (main.in.ready()) {
                    String msg = "";
                    while (main.in.ready()) {
                        del++;
                        msg = main.in.readLine();
                        if (del >= 10) {
                            break;
                        }
                    }
                    main.posL.setText("Now:     " + msg.replace("EQ", "X").replace("AZ", "Y"));
                }
                if (main.rotate) {
                    int[] pos = NetworkHandler.getSteps();

                    if (pos.length == 0) {
                        System.out.println("No Target");
                    } else {
                        while (Math.abs(main.posX - pos[0] + 15560) < Math.abs(main.posX - pos[0])) {
                            pos[0] -= 15560;
                        }
                        while (Math.abs(main.posX - pos[0] - 15560) < Math.abs(main.posX - pos[0])) {
                            pos[0] += 15560;
                        }
                        main.posX = pos[0];
                        main.posY = pos[1];
                        main.targetPosL.setText("Target: X: " + main.posX + " Y: " + main.posY);
                        main.rotateL.setText("<html>Follow: " + main.rotate + "<br/>Name: " + NetworkHandler.name + "<br/>Typ: " + NetworkHandler.typ + "</html>");
                        if (System.currentTimeMillis() - time > 300) {
                            main.out.print("?" + (main.posX + main.offsetX) + ";" + (main.posY + main.offsetY) + ";");
                            main.out.flush();
                            time = System.currentTimeMillis();
                        }
                    }
                } else {
                    if (System.currentTimeMillis() - time > 300) {
                        main.out.print("?" + (main.posX + main.offsetX) + ";" + (main.posY + main.offsetY) + ";");
                        main.out.flush();
                        System.out.println((main.posX + main.offsetX) + ";" + (main.posY + main.offsetY) + ";");
                        time = System.currentTimeMillis();
                    }
                }
            }

        }


    }

    public SerialPort connect() {
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            System.out.println(port.toString());
            if (port.toString().equals("Arduino Due Prog. Port")) {
                this.window.remove(connectB);
                this.window.setTitle("Teleskop - Move - CONNECTED");
                this.window.add(moveB);
                this.window.add(centerB);
                this.window.add(rotateB);
                this.window.add(offsetL);
                this.window.add(posL);
                this.window.add(rotateL);
                this.window.add(targetPosL);
                this.window.remove(connectB);
                this.window.add(textSpeed);
                this.window.repaint();
                connected = true;
                port.setBaudRate(115200);
                return port;
            }
        }
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.connectB) {

            arduino = connect();
            arduino.openPort();
            arduino.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
            out = new PrintWriter(arduino.getOutputStream());
            in = new BufferedReader(new InputStreamReader(arduino.getInputStream()));
        }
        if (e.getSource() == this.centerB) {
            try {
                int[] cent = NetworkHandler.getSteps();
                posY = cent[1];
                posX = cent[0];
                this.targetPosL.setText("Target: X: " + posX + " Y: " + posY);
                Thread.sleep(300);
                this.out.print("!" + posX + ";" + posY + ";");
                this.out.flush();
            } catch (Exception ee) {
                System.out.println("No Target");
            }

        }
        if (e.getSource() == this.rotateB) {
            rotate = !rotate;
            if (!rotate) {
                offsetX = 0;
                offsetY = 0;
                this.offsetL.setText("Offset: X: " + offsetX + " Y: " + offsetY);
            }
            rotateL.setText("<html>Follow: " + rotate + "<br/>Name: " + NetworkHandler.name + "<br/>Typ: " + NetworkHandler.typ + "</html>");
        }
    }

    public int[] toPoint(float deg) {
        double ang = Math.toRadians(deg);
        int y = (int) (Math.cos(ang) * radius);
        int x = (int) (Math.sin(ang) * radius);
        return new int[]{x, y};
    }

    public float toAngle() {
        float angle = (float) Math.toDegrees(Math.acos(posY / radius));
        if (posX < 0) {
            angle *= -1;
        }
        return angle;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        try {
            if (e.getKeyChar() == 'w') {
                if (rotate) {
                    offsetY++;
                } else {
                    posY++;
                }
            }
            if (e.getKeyChar() == 'a') {
                if (rotate) {
                    offsetX--;
                } else {
                    posX--;
                }
            }
            if (e.getKeyChar() == 's') {
                if (rotate) {
                    offsetY--;
                } else {
                    posY--;
                }
            }
            if (e.getKeyChar() == 'd') {
                if (rotate) {
                    offsetX++;
                } else {
                    posX++;
                }
            }
            if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                try {
                    speedMot = Integer.parseInt(textSpeed.getText());
                    System.out.println(speedMot);
                    Thread.sleep(300);
                    this.out.print("#" + speedMot + ";");
                    this.out.flush();
                    textSpeed.setText(speedMot + "");
                } catch (Exception ee) {
                    textSpeed.setText(speedMot + "");
                }
            }
            this.targetPosL.setText("Target: X: " + posX + " Y: " + posY);
            this.offsetL.setText("Offset: X: " + offsetX + " Y: " + offsetY);
        } catch (Exception ignored) {

        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
