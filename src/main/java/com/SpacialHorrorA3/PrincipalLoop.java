package com.SpacialHorrorA3;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.FPSAnimator;

/**
 * Abre a janela da animação em loop (AnimacaoLoop). O Principal original continua igual.
 *
 * Rodar:  mvn compile exec:java -Dexec.mainClass=com.SpacialHorrorA3.PrincipalLoop
 */
public class PrincipalLoop {

    public static void main(String[] args) {
        // Desativa a aceleração conflitante do Java 2D no Windows
        System.setProperty("sun.java2d.noddraw", "true");
        System.setProperty("sun.awt.noerasebackground", "true");

        // Puxa o perfil programável da GPU
        GLProfile profile = GLProfile.getMaxProgrammable(true);
        GLCapabilities capabilities = new GLCapabilities(profile);
        capabilities.setSampleBuffers(true); // antisserrilhado (bordas mais suaves)
        capabilities.setNumSamples(4);

        // Usa o GLJPanel para evitar crash com múltiplas GPUs
        GLJPanel canvas = new GLJPanel(capabilities);
        JFrame frame = new JFrame("Animação Espacial 3D - Loop");
        FPSAnimator animator = new FPSAnimator(canvas, 60, true);

        // Só é chamado se AnimacaoLoop.REPETIR_EM_LOOP for false: para o animador e fecha a janela
        canvas.addGLEventListener(new AnimacaoLoop(() -> new Thread(() -> {
            animator.stop();
            SwingUtilities.invokeLater(() -> {
                frame.dispose();
                System.exit(0);
            });
        }).start()));

        frame.getContentPane().add(canvas);
        frame.setSize(1290, 1080);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        animator.start();
    }
}
