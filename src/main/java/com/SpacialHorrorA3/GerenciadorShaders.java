package com.SpacialHorrorA3;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.jogamp.opengl.GL4;

public class GerenciadorShaders {

    public static int criarPrograma(GL4 gl, String caminhoVertex, String caminhoFragment) {
        int vertexShader = compilarShader(gl, caminhoVertex, GL4.GL_VERTEX_SHADER);
        int fragmentShader = compilarShader(gl, caminhoFragment, GL4.GL_FRAGMENT_SHADER);

        int idPrograma = gl.glCreateProgram();
        gl.glAttachShader(idPrograma, vertexShader);
        gl.glAttachShader(idPrograma, fragmentShader);
        gl.glLinkProgram(idPrograma);

        int[] linkado = new int[1];
        gl.glGetProgramiv(idPrograma, GL4.GL_LINK_STATUS, linkado, 0);
        if (linkado[0] == 0) {
            int[] tamanhoLog = new int[1];
            gl.glGetProgramiv(idPrograma, GL4.GL_INFO_LOG_LENGTH, tamanhoLog, 0);
            byte[] log = new byte[tamanhoLog[0]];
            gl.glGetProgramInfoLog(idPrograma, tamanhoLog[0], tamanhoLog, 0, log, 0);
            System.err.println("Erro ao linkar o programa: " + new String(log));
        }

        gl.glDeleteShader(vertexShader);
        gl.glDeleteShader(fragmentShader);

        return idPrograma;
    }

    private static int compilarShader(GL4 gl, String caminhoArquivo, int tipoShader) {
        String codigoFonte = "";
        try {
            codigoFonte = new String(Files.readAllBytes(Paths.get(caminhoArquivo)));
        } catch (Exception e) {
            System.err.println("Erro ao ler o arquivo: " + caminhoArquivo);
            return 0;
        }

        int idShader = gl.glCreateShader(tipoShader);
        gl.glShaderSource(idShader, 1, new String[]{codigoFonte}, null);
        gl.glCompileShader(idShader);

        int[] compilado = new int[1];
        gl.glGetShaderiv(idShader, GL4.GL_COMPILE_STATUS, compilado, 0);
        if (compilado[0] == 0) {
            int[] tamanhoLog = new int[1];
            gl.glGetShaderiv(idShader, GL4.GL_INFO_LOG_LENGTH, tamanhoLog, 0);
            byte[] log = new byte[tamanhoLog[0]];
            gl.glGetShaderInfoLog(idShader, tamanhoLog[0], tamanhoLog, 0, log, 0);
            System.err.println("Erro ao compilar " + caminhoArquivo + ":\n" + new String(log));
        }

        return idShader;
    }
}
