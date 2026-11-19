package com.SpacialHorrorA3;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

public class Renderizador implements GLEventListener {

    private int vaoNave, vboNave, qtdVerticesNave;
    private int vaoMonstro, vboMonstro, qtdVerticesMonstro;
    private int vaoEstrelas, vboEstrelas, qtdEstrelas = 1800;
    private int vaoFeixeLuz, vboFeixeLuz, qtdVerticesFeixe;

    private int vaoSol, vboSol, qtdVerticesSol;
    private int vaoPlaneta1, vboPlaneta1, qtdVerticesPlaneta1;
    private int vaoPlaneta2, vboPlaneta2, qtdVerticesPlaneta2;
    private int vaoPlaneta3, vboPlaneta3, qtdVerticesPlaneta3;

    private int shaderProgram;
    private long tempoInicial;

    @Override
    public void init(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();
        tempoInicial = System.currentTimeMillis();

        int[] vaos = new int[8];
        int[] vbos = new int[8];
        gl.glGenVertexArrays(8, vaos, 0);
        gl.glGenBuffers(8, vbos, 0);

        vaoNave = vaos[0];     vboNave = vbos[0];
        vaoMonstro = vaos[1];  vboMonstro = vbos[1];
        vaoEstrelas = vaos[2]; vboEstrelas = vbos[2];
        vaoFeixeLuz = vaos[3]; vboFeixeLuz = vbos[3];
        vaoSol = vaos[4];      vboSol = vbos[4];
        vaoPlaneta1 = vaos[5]; vboPlaneta1 = vbos[5];
        vaoPlaneta2 = vaos[6]; vboPlaneta2 = vbos[6];
        vaoPlaneta3 = vaos[7]; vboPlaneta3 = vbos[7];

        // Carrega a Nave
        qtdVerticesNave = carregarBufferModelo(gl, vaoNave, vboNave, "Nave_01.obj", new float[]{0.8f, 0.8f, 0.8f});

        // Carrega o Monstro
        qtdVerticesMonstro = carregarBufferModelo(gl, vaoMonstro, vboMonstro, "space-monster-final1.obj", new float[]{0.08f, 0.20f, 0.25f});

        // Carrega o Sol e os 3 Planetas
        qtdVerticesSol = carregarBufferModelo(gl, vaoSol, vboSol, "Sol.obj", new float[]{1.0f, 0.78f, 0.15f});
        qtdVerticesPlaneta1 = carregarBufferModelo(gl, vaoPlaneta1, vboPlaneta1, "planeta1.obj", new float[]{0.20f, 0.55f, 0.25f});
        qtdVerticesPlaneta2 = carregarBufferModelo(gl, vaoPlaneta2, vboPlaneta2, "planet2.obj", new float[]{0.38f, 0.16f, 0.12f});
        qtdVerticesPlaneta3 = carregarBufferModelo(gl, vaoPlaneta3, vboPlaneta3, "planeta3.obj", new float[]{0.82f, 0.68f, 0.45f});

        // Gera estrelas e partículas abissais de fundo
        float[] dadosEstrelas = gerarEstrelas(qtdEstrelas);
        FloatBuffer bufferEstrelas = Buffers.newDirectFloatBuffer(dadosEstrelas);
        gl.glBindVertexArray(vaoEstrelas);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vboEstrelas);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, bufferEstrelas.limit() * 4L, bufferEstrelas, GL4.GL_STATIC_DRAW);

        // Gera o cone volumétrico do farol da nave
        float[] dadosFeixe = gerarConeHolofote();
        qtdVerticesFeixe = dadosFeixe.length / 9;
        FloatBuffer bufferFeixe = Buffers.newDirectFloatBuffer(dadosFeixe);
        gl.glBindVertexArray(vaoFeixeLuz);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vboFeixeLuz);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, bufferFeixe.limit() * 4L, bufferFeixe, GL4.GL_STATIC_DRAW);

        // Compila os Shaders
        shaderProgram = GerenciadorShaders.criarPrograma(gl,
            "src/main/resources/shaders/vertex.glsl",
            "src/main/resources/shaders/fragment.glsl"
        );
    }

    private int carregarBufferModelo(GL4 gl, int vao, int vbo, String arquivo, float[] corPadrao) {
        float[] dados = OBJLoader.carregarDadosModelo(arquivo, corPadrao);
        if (dados.length == 0) return 0;
        FloatBuffer buffer = Buffers.newDirectFloatBuffer(dados);
        gl.glBindVertexArray(vao);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vbo);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, buffer.limit() * 4L, buffer, GL4.GL_STATIC_DRAW);
        return dados.length / 9;
    }

    private float[] gerarConeHolofote() {
        int fatias = 24;
        float comprimento = 24.0f;
        float raioFinal = 6.5f;
        List<Float> lista = new ArrayList<>();

        for (int i = 0; i < fatias; i++) {
            double ang1 = (2.0 * Math.PI * i) / fatias;
            double ang2 = (2.0 * Math.PI * (i + 1)) / fatias;

            float x1 = (float) Math.cos(ang1) * raioFinal;
            float y1 = (float) Math.sin(ang1) * raioFinal;
            float x2 = (float) Math.cos(ang2) * raioFinal;
            float y2 = (float) Math.sin(ang2) * raioFinal;

            adicionarVerticeSimples(lista, 0.0f, 0.0f, -7.0f, 0.75f, 0.95f, 1.0f);
            adicionarVerticeSimples(lista, x1, y1, -7.0f - comprimento, 0.35f, 0.75f, 0.90f);
            adicionarVerticeSimples(lista, x2, y2, -7.0f - comprimento, 0.35f, 0.75f, 0.90f);
        }

        float[] arr = new float[lista.size()];
        for (int i = 0; i < lista.size(); i++) arr[i] = lista.get(i);
        return arr;
    }

    private void adicionarVerticeSimples(List<Float> lista, float x, float y, float z, float r, float g, float b) {
        lista.add(x); lista.add(y); lista.add(z);
        lista.add(r); lista.add(g); lista.add(b);
        lista.add(0.0f); lista.add(1.0f); lista.add(0.0f);
    }

    private float[] gerarEstrelas(int quantidade) {
        Random rand = new Random(42);
        float[] dados = new float[quantidade * 6];

        for (int i = 0; i < quantidade; i++) {
            double theta = rand.nextDouble() * 2.0 * Math.PI;
            double phi = Math.acos(2.0 * rand.nextDouble() - 1.0);
            double raio = 60.0 + rand.nextDouble() * 220.0;

            float x = (float) (raio * Math.sin(phi) * Math.cos(theta));
            float y = (float) (raio * Math.sin(phi) * Math.sin(theta));
            float z = (float) (raio * Math.cos(phi));

            float brilho = 0.35f + rand.nextFloat() * 0.65f;
            float r = brilho, g = brilho, b = brilho;

            int tipoCor = rand.nextInt(8);
            if (tipoCor <= 2) {
                r *= 0.45f;
                g *= 0.90f;
            } else if (tipoCor == 3) {
                b *= 0.75f;
            }

            int base = i * 6;
            dados[base] = x;
            dados[base + 1] = y;
            dados[base + 2] = z;
            dados[base + 3] = r;
            dados[base + 4] = g;
            dados[base + 5] = b;
        }
        return dados;
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();

        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glDisable(GL4.GL_CULL_FACE);

        gl.glClearColor(0.005f, 0.008f, 0.015f, 1.0f);
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);

        gl.glUseProgram(shaderProgram);

        int largura = Math.max(1, drawable.getSurfaceWidth());
        int altura = Math.max(1, drawable.getSurfaceHeight());
        float proporcaoTela = (float) largura / altura;

        Vector3f posCamera = new Vector3f(0.0f, 2.0f, 24.0f);
        Matrix4f projecao = new Matrix4f().setPerspective((float) Math.toRadians(45.0f), proporcaoTela, 0.1f, 1000.0f);
        Matrix4f visualizacao = new Matrix4f().setLookAt(
            posCamera.x, posCamera.y, posCamera.z,
            0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f
        );

        float tempo = (System.currentTimeMillis() - tempoInicial) / 1000.0f;

        int locMVP = gl.glGetUniformLocation(shaderProgram, "matrizMVP");
        int locModelo = gl.glGetUniformLocation(shaderProgram, "matrizModelo");
        int locIluminacao = gl.glGetUniformLocation(shaderProgram, "usarIluminacao");
        int locPosCamera = gl.glGetUniformLocation(shaderProgram, "posicaoCamera");
        int locPosLuzNave = gl.glGetUniformLocation(shaderProgram, "posicaoLuzNave");
        int locDirLuzNave = gl.glGetUniformLocation(shaderProgram, "direcaoLuzNave");

        gl.glUniform3f(locPosCamera, posCamera.x, posCamera.y, posCamera.z);

        float naveX = -8.5f;
        float naveY = 1.2f + (float) Math.sin(tempo * 1.5f) * 0.4f;
        float naveZ = 3.5f;

        Vector3f posFarol = new Vector3f(naveX + 1.8f, naveY, naveZ - 0.5f);
        Vector3f alvoMonstro = new Vector3f(3.5f, 0.2f, 0.5f);
        Vector3f dirFarol = new Vector3f(alvoMonstro).sub(posFarol).normalize();

        gl.glUniform3f(locPosLuzNave, posFarol.x, posFarol.y, posFarol.z);
        gl.glUniform3f(locDirLuzNave, dirFarol.x, dirFarol.y, dirFarol.z);

        float[] arrayMVP = new float[16];
        float[] arrayModelo = new float[16];

        // --- ESTRELAS E PARTÍCULAS ABISSAIS ---
        Matrix4f modeloEstrelas = new Matrix4f().identity().rotateY(tempo * 0.015f);
        Matrix4f mvpEstrelas = new Matrix4f();
        projecao.mul(visualizacao, mvpEstrelas);
        mvpEstrelas.mul(modeloEstrelas);

        mvpEstrelas.get(arrayMVP);
        modeloEstrelas.get(arrayModelo);
        gl.glUniformMatrix4fv(locMVP, 1, false, arrayMVP, 0);
        gl.glUniformMatrix4fv(locModelo, 1, false, arrayModelo, 0);
        gl.glUniform1i(locIluminacao, 0);

        gl.glPointSize(2.0f);
        gl.glBindVertexArray(vaoEstrelas);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vboEstrelas);
        int strideEstrelas = 6 * 4;
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, strideEstrelas, 0);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, strideEstrelas, 3 * 4L);
        gl.glEnableVertexAttribArray(1);
        gl.glDisableVertexAttribArray(2);

        gl.glDrawArrays(GL4.GL_POINTS, 0, qtdEstrelas);

        // --- SISTEMA PLANETÁRIO AO FUNDO (Modo 1: Iluminação difusa + Emissão para Sol/Lava) ---
        gl.glUniform1i(locIluminacao, 1);

        // Posição base do Sol distante ao fundo à esquerda/topo
        Vector3f centroSistema = new Vector3f(-22.0f, 11.0f, -55.0f);

        if (qtdVerticesSol > 0) {
            Matrix4f modeloSol = new Matrix4f()
                .identity()
                .translate(centroSistema)
                .rotateY(tempo * 0.25f)
                .scale(0.35f);
            desenharMalha(gl, vaoSol, vboSol, qtdVerticesSol, projecao, visualizacao, modeloSol, locMVP, locModelo, arrayMVP, arrayModelo);
        }

        if (qtdVerticesPlaneta2 > 0) { // Planeta Vulcânico (órbita interna)
            Matrix4f modeloPlaneta2 = new Matrix4f()
                .identity()
                .translate(centroSistema)
                .rotateY(tempo * 0.65f)
                .translate(12.0f, -1.5f, 0.0f)
                .rotateY(tempo * 1.4f)
                .scale(0.18f);
            desenharMalha(gl, vaoPlaneta2, vboPlaneta2, qtdVerticesPlaneta2, projecao, visualizacao, modeloPlaneta2, locMVP, locModelo, arrayMVP, arrayModelo);
        }

        if (qtdVerticesPlaneta1 > 0) { // Planeta Terra com Árvores e Lua (órbita média)
            Matrix4f modeloPlaneta1 = new Matrix4f()
                .identity()
                .translate(centroSistema)
                .rotateY(tempo * 0.35f + 1.2f)
                .translate(24.0f, 2.0f, 0.0f)
                .rotateY(tempo * 0.9f)
                .scale(0.22f);
            desenharMalha(gl, vaoPlaneta1, vboPlaneta1, qtdVerticesPlaneta1, projecao, visualizacao, modeloPlaneta1, locMVP, locModelo, arrayMVP, arrayModelo);
        }

        if (qtdVerticesPlaneta3 > 0) { // Planeta com Anéis (órbita externa à direita ao fundo)
            Matrix4f modeloPlaneta3 = new Matrix4f()
                .identity()
                .translate(centroSistema)
                .rotateY(tempo * 0.18f + 0.4f)
                .translate(42.0f, -4.0f, 0.0f)
                .rotateZ((float) Math.toRadians(18.0f))
                .rotateY(tempo * 0.7f)
                .scale(0.25f);
            desenharMalha(gl, vaoPlaneta3, vboPlaneta3, qtdVerticesPlaneta3, projecao, visualizacao, modeloPlaneta3, locMVP, locModelo, arrayMVP, arrayModelo);
        }

        // --- NAVE EXPLORADORA (À esquerda, apontando para a criatura) ---
        gl.glUniform1i(locIluminacao, 1);

        Matrix4f modeloNave = new Matrix4f()
            .identity()
            .translate(naveX, naveY, naveZ)
            .rotateY((float) Math.toRadians(-75.0f))
            .rotateX((float) Math.toRadians(-8.0f))
            .rotateZ((float) Math.sin(tempo * 1.5f) * 0.08f)
            .scale(0.32f);

        desenharMalha(gl, vaoNave, vboNave, qtdVerticesNave, projecao, visualizacao, modeloNave, locMVP, locModelo, arrayMVP, arrayModelo);

        // --- CRIATURA ESPACIAL (À direita, tamanho ajustado e olhando para a nave) ---
        if (qtdVerticesMonstro > 0) {
            gl.glUniform1i(locIluminacao, 2);

            Matrix4f modeloMonstro = new Matrix4f()
                .identity()
                .translate(4.5f, -0.5f + (float) Math.cos(tempo * 1.1f) * 0.4f, -1.5f)
                .rotateY((float) Math.toRadians(-52.0f) + (float) Math.sin(tempo * 0.6f) * 0.12f)
                .rotateX((float) Math.toRadians(8.0f))
                .scale(0.45f);

            desenharMalha(gl, vaoMonstro, vboMonstro, qtdVerticesMonstro, projecao, visualizacao, modeloMonstro, locMVP, locModelo, arrayMVP, arrayModelo);
        }

        // --- FEIXE DE LUZ VOLUMÉTRICO DO FAROL DA NAVE ---
        gl.glEnable(GL4.GL_BLEND);
        gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_ONE);
        gl.glDepthMask(false);

        gl.glUniform1i(locIluminacao, 3);
        desenharMalha(gl, vaoFeixeLuz, vboFeixeLuz, qtdVerticesFeixe, projecao, visualizacao, modeloNave, locMVP, locModelo, arrayMVP, arrayModelo);

        gl.glDepthMask(true);
        gl.glDisable(GL4.GL_BLEND);
    }

    private void desenharMalha(GL4 gl, int vao, int vbo, int qtdVertices,
                               Matrix4f projecao, Matrix4f visualizacao, Matrix4f modelo,
                               int locMVP, int locModelo, float[] arrayMVP, float[] arrayModelo) {
        Matrix4f mvp = new Matrix4f();
        projecao.mul(visualizacao, mvp);
        mvp.mul(modelo);

        mvp.get(arrayMVP);
        modelo.get(arrayModelo);
        gl.glUniformMatrix4fv(locMVP, 1, false, arrayMVP, 0);
        gl.glUniformMatrix4fv(locModelo, 1, false, arrayModelo, 0);

        gl.glBindVertexArray(vao);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vbo);
        int stride = 9 * 4;
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, stride, 0);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, stride, 3 * 4L);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(2, 3, GL4.GL_FLOAT, false, stride, 6 * 4L);
        gl.glEnableVertexAttribArray(2);

        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, qtdVertices);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL4 gl = drawable.getGL().getGL4();
        gl.glViewport(x, y, width, height);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();
        gl.glDeleteVertexArrays(8, new int[]{vaoNave, vaoMonstro, vaoEstrelas, vaoFeixeLuz, vaoSol, vaoPlaneta1, vaoPlaneta2, vaoPlaneta3}, 0);
        gl.glDeleteBuffers(8, new int[]{vboNave, vboMonstro, vboEstrelas, vboFeixeLuz, vboSol, vboPlaneta1, vboPlaneta2, vboPlaneta3}, 0);
    }
}