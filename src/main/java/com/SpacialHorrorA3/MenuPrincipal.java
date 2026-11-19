package com.SpacialHorrorA3;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.FPSAnimator;

public class MenuPrincipal {

    private static final Color COR_FUNDO = new Color(8, 12, 22);
    private static final Color COR_CARD = new Color(16, 24, 42);
    private static final Color COR_CARD_HOVER = new Color(24, 42, 68);
    private static final Color COR_CIANO = new Color(75, 225, 245);
    private static final Color COR_TEXTO = new Color(230, 238, 248);
    private static final Color COR_SUBTEXTO = new Color(150, 170, 195);

    private JFrame frameMenu;

    public static void main(String[] args) {
        // Deve ser configurado antes de abrir qualquer componente Swing/JOGL no Windows
        System.setProperty("sun.java2d.noddraw", "true");
        System.setProperty("sun.awt.noerasebackground", "true");

        SwingUtilities.invokeLater(() -> new MenuPrincipal().exibirMenu());
    }

    private void exibirMenu() {
        frameMenu = new JFrame("SpacialHorrorA3 — Seleção de Cena");
        frameMenu.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frameMenu.setSize(680, 420);
        frameMenu.setResizable(false);
        frameMenu.setLocationRelativeTo(null);

        JPanel painelRaiz = new JPanel(new BorderLayout(0, 20));
        painelRaiz.setBackground(COR_FUNDO);
        painelRaiz.setBorder(BorderFactory.createEmptyBorder(30, 36, 26, 36));

        // Cabeçalho
        JPanel painelTopo = new JPanel();
        painelTopo.setLayout(new BoxLayout(painelTopo, BoxLayout.Y_AXIS));
        painelTopo.setBackground(COR_FUNDO);

        JLabel titulo = new JLabel("SPACIAL HORROR 3D");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titulo.setForeground(COR_CIANO);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitulo = new JLabel("Escolha qual cena deseja visualizar em OpenGL 4.0");
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitulo.setForeground(COR_SUBTEXTO);
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        painelTopo.add(titulo);
        painelTopo.add(Box.createVerticalStrut(6));
        painelTopo.add(subtitulo);

        // Botões das cenas
        JPanel painelBotoes = new JPanel(new GridLayout(1, 2, 20, 0));
        painelBotoes.setBackground(COR_FUNDO);

        JButton btnOriginal = criarBotaoCena(
            "Cena Estática",
            "Encontro estático entre a nave exploradora, o holofote volumétrico, a criatura abissal e os planetas ao fundo."
        );
        btnOriginal.addActionListener(e -> abrirCena(
            "Animação Espacial 3D — Cena Original",
            new Renderizador(),
            false
        ));

        JButton btnLoop = criarBotaoCena(
            "Animação em Loop",
            "Curta cinematográfico em loop (67s): salto no hiperespaço, sobrevoo dos planetas, a criatura devora o Sol e a fuga."
        );
        btnLoop.addActionListener(e -> abrirCenaLoop());

        painelBotoes.add(btnOriginal);
        painelBotoes.add(btnLoop);

        // Rodapé
        JLabel rodape = new JLabel("Dica: pressione ESC ou feche a janela da cena para retornar a este menu", SwingConstants.CENTER);
        rodape.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        rodape.setForeground(COR_SUBTEXTO);

        painelRaiz.add(painelTopo, BorderLayout.NORTH);
        painelRaiz.add(painelBotoes, BorderLayout.CENTER);
        painelRaiz.add(rodape, BorderLayout.SOUTH);

        frameMenu.setContentPane(painelRaiz);
        frameMenu.setVisible(true);
    }

    private JButton criarBotaoCena(String titulo, String descricao) {
        String html = "<html><div style='text-align:center; padding:8px;'>"
            + "<div style='font-size:15px; font-weight:bold; color:#4BE1F5; margin-bottom:10px;'>" + titulo + "</div>"
            + "<div style='font-size:11px; color:#DCE6F2; line-height:1.35;'>" + descricao + "</div>"
            + "</div></html>";

        JButton btn = new JButton(html);
        btn.setFocusPainted(false);
        btn.setBackground(COR_CARD);
        btn.setForeground(COR_TEXTO);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(260, 170));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(40, 95, 130), 2),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(COR_CARD_HOVER);
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(COR_CIANO, 2),
                    BorderFactory.createEmptyBorder(12, 14, 12, 14)
                ));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(COR_CARD);
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(40, 95, 130), 2),
                    BorderFactory.createEmptyBorder(12, 14, 12, 14)
                ));
            }
        });

        return btn;
    }

    private void abrirCenaLoop() {
        AtomicBoolean fechando = new AtomicBoolean(false);
        JFrame frameCena = new JFrame("Animação Espacial 3D — Loop");

        GLJPanel canvas = criarCanvasOpenGL(true);
        FPSAnimator animator = new FPSAnimator(canvas, 60, true);

        Runnable voltarAoMenu = () -> encerrarCenaEVoltar(fechando, animator, frameCena);

        // Caso AnimacaoLoop.REPETIR_EM_LOOP seja false, volta ao menu ao terminar
        canvas.addGLEventListener(new AnimacaoLoop(voltarAoMenu));
        configurarEExibirJanelaCena(frameCena, canvas, animator, voltarAoMenu);
    }

    private void abrirCena(String tituloJanela, GLEventListener cena, boolean usarAntialiasing) {
        AtomicBoolean fechando = new AtomicBoolean(false);
        JFrame frameCena = new JFrame(tituloJanela);

        GLJPanel canvas = criarCanvasOpenGL(usarAntialiasing);
        FPSAnimator animator = new FPSAnimator(canvas, 60, true);

        Runnable voltarAoMenu = () -> encerrarCenaEVoltar(fechando, animator, frameCena);

        canvas.addGLEventListener(cena);
        configurarEExibirJanelaCena(frameCena, canvas, animator, voltarAoMenu);
    }

    private GLJPanel criarCanvasOpenGL(boolean usarAntialiasing) {
        GLProfile profile = GLProfile.getMaxProgrammable(true);
        GLCapabilities capabilities = new GLCapabilities(profile);
        if (usarAntialiasing) {
            capabilities.setSampleBuffers(true);
            capabilities.setNumSamples(4);
        }
        return new GLJPanel(capabilities);
    }

    private void configurarEExibirJanelaCena(JFrame frameCena, GLJPanel canvas, FPSAnimator animator, Runnable voltarAoMenu) {
        frameMenu.setVisible(false);

        frameCena.getContentPane().add(canvas);
        frameCena.setSize(1290, 1080);
        frameCena.setLocationRelativeTo(null);
        frameCena.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        frameCena.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                voltarAoMenu.run();
            }
        });

        KeyAdapter teclaEsc = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    voltarAoMenu.run();
                }
            }
        };
        frameCena.addKeyListener(teclaEsc);
        canvas.addKeyListener(teclaEsc);
        canvas.setFocusable(true);

        frameCena.setVisible(true);
        canvas.requestFocusInWindow();
        animator.start();
    }

    private void encerrarCenaEVoltar(AtomicBoolean fechando, FPSAnimator animator, JFrame frameCena) {
        if (!fechando.compareAndSet(false, true)) return;

        // Para o FPSAnimator fora da EDT para evitar deadlock com o GLJPanel
        new Thread(() -> {
            if (animator.isStarted()) {
                animator.stop();
            }
            SwingUtilities.invokeLater(() -> {
                frameCena.dispose();
                frameMenu.setVisible(true);
                frameMenu.toFront();
            });
        }).start();
    }
}