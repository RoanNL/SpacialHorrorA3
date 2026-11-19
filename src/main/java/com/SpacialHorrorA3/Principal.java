package com.SpacialHorrorA3;

import javax.swing.JFrame;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.FPSAnimator;

public class Principal {

    public static void main(String[] args) {
        // Desativa a aceleração conflitante do Java 2D no Windows
        System.setProperty("sun.java2d.noddraw", "true");
        System.setProperty("sun.awt.noerasebackground", "true");

        // Puxa o perfil programável da GPU
        GLProfile profile = GLProfile.getMaxProgrammable(true);
        GLCapabilities capabilities = new GLCapabilities(profile);

        // Usa o GLJPanel para evitar crash com múltiplas GPUs
        GLJPanel canvas = new GLJPanel(capabilities);
        canvas.addGLEventListener(new Renderizador());

        JFrame frame = new JFrame("Animação Espacial 3D");
        frame.getContentPane().add(canvas);
        frame.setSize(1290, 1080);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        FPSAnimator animator = new FPSAnimator(canvas, 60, true);
        animator.start();
    }
}