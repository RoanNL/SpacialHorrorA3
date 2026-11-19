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

/**
 * Animação em loop "Spacial Horror" (não altera nenhuma classe original do projeto:
 * usa o OBJLoader e o GerenciadorShaders como estão, com shaders próprios).
 *
 *  1. Um buraco de hiperespaço se abre e a nave sai dele, num sistema solar.
 *  2. A nave passa pelos três planetas (bem maiores que ela) e para diante do Sol, maior que todos.
 *  3. A criatura se forma no meio das estrelas e devora o Sol.
 *  4. No escuro, a criatura avança sobre a nave; outro buraco se abre e a nave foge pelo hiperespaço.
 *
 * O ciclo começa e termina DENTRO do túnel do hiperespaço, que cobre a tela inteira:
 * o último quadro é idêntico ao primeiro, então o loop se repete sem emenda.
 *
 * Tudo é função do tempo em segundos dentro do ciclo. Para mudar o ritmo, ajuste as constantes T_*.
 * Para começar do meio (útil enquanto ajusta), rode com -Dinicio=40 (segundos).
 */
public class AnimacaoLoop implements GLEventListener {

    // ======================= ROTEIRO (segundos) =======================
    static final float PERIODO            = 67.0f;  // duração de um ciclo do loop
    static final boolean REPETIR_EM_LOOP  = true;   // false = toca um ciclo e chama aoTerminar

    static final float T_SAI_HIPERESPACO  = 1.6f;   // a nave sai do buraco de chegada
    static final float T_BURACO_FECHA     = 3.0f;   // o buraco de chegada se fecha atrás dela
    static final float T_PLANETA_ANEIS    = 10.0f;  // passa sobre os anéis do planeta gasoso
    static final float T_PLANETA_LAVA     = 20.0f;  // rasante pelo planeta vulcânico, perto do Sol
    static final float T_PLANETA_TERRA    = 29.0f;  // passa por fora do planeta com lua e faz a curva
    static final float T_PARADA           = 37.0f;  // para diante do Sol (contemplação)
    static final float T_CRIATURA_SURGE   = 39.5f;  // criatura começa a se formar no meio das estrelas
    static final float DURACAO_SURGIMENTO = 8.0f;
    static final float T_FAROL_LIGA       = 46.0f;  // nave percebe e liga o farol
    static final float T_BOCA_ABRE        = 50.5f;
    static final float T_CRIATURA_CHEGA   = 52.5f;  // cabeça chega ao lado do Sol
    static final float T_DEVORAR_INICIO   = 53.0f;  // Sol começa a ser sugado
    static final float T_DEVORAR_FIM      = 57.5f;  // Sol some dentro da boca
    static final float T_BOCA_FECHA       = 57.9f;
    static final float T_FAROL_FALHA      = 59.5f;  // farol pisca e apaga
    static final float T_NAVE_FOGE        = 60.5f;  // nave dá meia-volta para fugir
    static final float T_BURACO_SAIDA_ABRE = 61.0f; // buraco de fuga se abre
    static final float T_BOTE             = 61.5f;  // criatura avança sobre a nave
    static final float T_ENTRA_HIPERESPACO = 66.0f; // nave mergulha no buraco; a câmera vai junto

    // ======================= ESCALAS =======================
    // A nave é minúscula perto dos planetas, e o Sol é maior que todos eles.
    private static final float ESCALA_NAVE = 0.06f;      // envergadura ~0,85
    private static final float ESCALA_SOL = 1.6f;        // raio ~16
    private static final float RAIO_SOL = 10.0f * ESCALA_SOL;
    private static final float ESCALA_CRIATURA = 3.0f;   // ~120 de comprimento
    private static final float RAIO_BURACO = 6.0f;

    private static final Vector3f CIMA = new Vector3f(0.0f, 1.0f, 0.0f);
    private static final Vector3f CENTRO_SOL = new Vector3f(0.0f, 0.0f, 0.0f);
    private static final Vector3f BURACO_CHEGADA = new Vector3f(-240.0f, 30.0f, 115.0f);
    private static final float RAIO_CURVA_FINAL = 10.0f;
    private static final float ALTURA_PARADA = 5.0f;

    // Pontos no espaço local da criatura (o .obj tem a cabeça voltada para +Z)
    private static final Vector3f BOCA_LOCAL = new Vector3f(0.0f, 0.5f, 12.0f);
    private static final Vector3f DOBRADICA_MANDIBULA = new Vector3f(0.0f, 0.45f, 11.2f);

    private static final Vector3f COR_SOL = new Vector3f(1.00f, 0.90f, 0.72f);
    private static final Vector3f COR_SOL_MORRENDO = new Vector3f(1.00f, 0.26f, 0.05f);

    private static final int QTD_ESTRELAS = 2600;

    /** Um planeta em órbita circular lenta ao redor do Sol. */
    private static final class Planeta {
        final String arquivo;
        final float[] corPadrao;
        final float raioOrbita, altura, escala, velocidadeOrbital, anguloNaPassagem, tPassagem, giro, inclinacaoGraus;
        int vao, vbo, qtd;

        Planeta(String arquivo, float[] corPadrao, float raioOrbita, float altura, float escala,
                float velocidadeOrbital, float anguloNaPassagem, float tPassagem, float giro, float inclinacaoGraus) {
            this.arquivo = arquivo;
            this.corPadrao = corPadrao;
            this.raioOrbita = raioOrbita;
            this.altura = altura;
            this.escala = escala;
            this.velocidadeOrbital = velocidadeOrbital;
            this.anguloNaPassagem = anguloNaPassagem;
            this.tPassagem = tPassagem;
            this.giro = giro;
            this.inclinacaoGraus = inclinacaoGraus;
        }

        /** Posição na órbita. O ângulo é escolhido para o planeta estar no caminho da nave em tPassagem. */
        Vector3f posicao(float t, Vector3f destino) {
            float a = anguloNaPassagem + velocidadeOrbital * (t - tPassagem);
            return destino.set(CENTRO_SOL).add(raioOrbita * (float) Math.cos(a), altura, raioOrbita * (float) Math.sin(a));
        }

        Matrix4f modelo(float t, Matrix4f destino) {
            return destino.identity()
                .translate(posicao(t, new Vector3f()))
                .rotateZ((float) Math.toRadians(inclinacaoGraus))
                .rotateY(t * giro)
                .scale(escala);
        }
    }

    // raio do planeta = 10 * escala (com lua/anéis indo além disso)
    private final Planeta planetaLava = new Planeta("planet2.obj", new float[]{0.38f, 0.16f, 0.12f},
        34.0f, 0.0f, 0.60f, 0.020f, 1.90f, T_PLANETA_LAVA, 0.40f, 0.0f);
    private final Planeta planetaTerra = new Planeta("planeta1.obj", new float[]{0.20f, 0.55f, 0.25f},
        62.0f, 2.0f, 0.75f, 0.012f, 0.90f, T_PLANETA_TERRA, 0.25f, 0.0f);
    private final Planeta planetaAneis = new Planeta("planeta3.obj", new float[]{0.82f, 0.68f, 0.45f},
        100.0f, -3.0f, 0.85f, 0.007f, 2.669f, T_PLANETA_ANEIS, 0.20f, 18.0f);

    // ======================= OPENGL =======================
    private int vaoNave, vboNave, qtdNave;
    private int vaoCriatura, vboCriatura, qtdCriatura;
    private int vaoMandibula, vboMandibula, qtdMandibula;
    private int vaoEstrelas, vboEstrelas;
    private int vaoFeixe, vboFeixe, qtdFeixe;
    private int vaoSol, vboSol, qtdSol;
    private int vaoQuad, vboQuad, qtdQuad;
    private int[] todosVaos, todosVbos;

    private int programa;
    private int locVP, locModelo, locModo, locPosCamera, locTempo, locPeriodo, locFade, locAspecto;
    private int locPosLuzNave, locDirLuzNave, locIntensidadeFarol, locAlcanceFeixe;
    private int locPosSol, locCorSol, locIntensidadeSol, locEnergia, locSurgimento, locIntensidade;
    private int locPuxao, locAlvoPuxao, locCentroPuxao;

    private final float[] arrayMatriz = new float[16];

    // ======================= TEMPO =======================
    private final Runnable aoTerminar;
    private boolean terminou = false;
    private long inicioNanos;
    private final float tempoInicial;

    // Trajetos calculados uma vez no init()
    private Trajeto trajetoNave;       // do buraco de chegada até parar diante do Sol
    private Trajeto trajetoFuga;       // da parada até o buraco de saída
    private Trajeto trajetoCriatura;
    private Vector3f pontoParada;
    private Vector3f frenteParada, direitaParada, cimaParada; // eixos da vista da nave parada olhando o Sol
    private Vector3f direcaoComer;
    private Vector3f direcaoSaidaChegada;  // para onde a nave sai do buraco de chegada
    private Vector3f buracoSaida;

    // Câmera: cada tomada começa num instante e entra misturando com a anterior por 'transição' segundos (0 = corte seco)
    private static final float[] TOMADA_INICIO    = {0.0f, 4.0f, 14.5f, 24.0f, 30.5f, 34.5f, 44.0f, 58.5f, 60.8f, 64.2f};
    private static final float[] TOMADA_TRANSICAO = {0.0f, 3.0f,  0.0f,  0.0f,  1.5f,  3.5f,  4.5f,  2.5f,  0.0f,  0.0f};

    public AnimacaoLoop() {
        this(null);
    }

    /** @param aoTerminar chamado quando o ciclo acaba, se REPETIR_EM_LOOP for false (pode ser null). */
    public AnimacaoLoop(Runnable aoTerminar) {
        this.aoTerminar = aoTerminar;
        float inicio = 0.0f;
        try {
            inicio = Float.parseFloat(System.getProperty("inicio", "0"));
        } catch (NumberFormatException ignored) {
        }
        this.tempoInicial = inicio;
    }

    // =====================================================================
    //                              INICIALIZAÇÃO
    // =====================================================================

    @Override
    public void init(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();

        todosVaos = new int[10];
        todosVbos = new int[10];
        gl.glGenVertexArrays(10, todosVaos, 0);
        gl.glGenBuffers(10, todosVbos, 0);
        vaoNave = todosVaos[0];       vboNave = todosVbos[0];
        vaoCriatura = todosVaos[1];   vboCriatura = todosVbos[1];
        vaoMandibula = todosVaos[2];  vboMandibula = todosVbos[2];
        vaoEstrelas = todosVaos[3];   vboEstrelas = todosVbos[3];
        vaoFeixe = todosVaos[4];      vboFeixe = todosVbos[4];
        vaoSol = todosVaos[5];        vboSol = todosVbos[5];
        vaoQuad = todosVaos[6];       vboQuad = todosVbos[6];
        planetaLava.vao = todosVaos[7];  planetaLava.vbo = todosVbos[7];
        planetaTerra.vao = todosVaos[8]; planetaTerra.vbo = todosVbos[8];
        planetaAneis.vao = todosVaos[9]; planetaAneis.vbo = todosVbos[9];

        // Modelos (carregados pelo OBJLoader original, sem modificá-lo)
        qtdNave = enviar(gl, vaoNave, vboNave, OBJLoader.carregarDadosModelo("Nave_01.obj", new float[]{0.8f, 0.8f, 0.8f}));

        float[][] partes = separarMandibula(OBJLoader.carregarDadosModelo("space-monster-final1.obj", new float[]{0.08f, 0.20f, 0.25f}));
        qtdCriatura = enviar(gl, vaoCriatura, vboCriatura, partes[0]);
        qtdMandibula = enviar(gl, vaoMandibula, vboMandibula, partes[1]);

        float[] sol = OBJLoader.carregarDadosModelo("Sol.obj", new float[]{1.0f, 0.78f, 0.15f});
        centralizarPelaCaixa(sol); // o loader centraliza o Sol só por uma parte da esfera; aqui usamos a esfera inteira
        qtdSol = enviar(gl, vaoSol, vboSol, sol);

        for (Planeta p : new Planeta[]{planetaLava, planetaTerra, planetaAneis}) {
            p.qtd = enviar(gl, p.vao, p.vbo, OBJLoader.carregarDadosModelo(p.arquivo, p.corPadrao));
        }

        // Geometrias geradas por código
        enviar(gl, vaoEstrelas, vboEstrelas, gerarEstrelas(QTD_ESTRELAS));
        qtdFeixe = enviar(gl, vaoFeixe, vboFeixe, gerarConeHolofote());
        qtdQuad = enviar(gl, vaoQuad, vboQuad, gerarQuadrado());

        programa = GerenciadorShaders.criarPrograma(gl,
            "src/main/resources/shaders/loop_vertex.glsl",
            "src/main/resources/shaders/loop_fragment.glsl"
        );
        locVP = gl.glGetUniformLocation(programa, "matrizVP");
        locModelo = gl.glGetUniformLocation(programa, "matrizModelo");
        locModo = gl.glGetUniformLocation(programa, "usarIluminacao");
        locPosCamera = gl.glGetUniformLocation(programa, "posicaoCamera");
        locTempo = gl.glGetUniformLocation(programa, "tempo");
        locPeriodo = gl.glGetUniformLocation(programa, "periodo");
        locFade = gl.glGetUniformLocation(programa, "fade");
        locAspecto = gl.glGetUniformLocation(programa, "aspecto");
        locPosLuzNave = gl.glGetUniformLocation(programa, "posicaoLuzNave");
        locDirLuzNave = gl.glGetUniformLocation(programa, "direcaoLuzNave");
        locIntensidadeFarol = gl.glGetUniformLocation(programa, "intensidadeFarol");
        locAlcanceFeixe = gl.glGetUniformLocation(programa, "alcanceFeixe");
        locPosSol = gl.glGetUniformLocation(programa, "posicaoSol");
        locCorSol = gl.glGetUniformLocation(programa, "corSol");
        locIntensidadeSol = gl.glGetUniformLocation(programa, "intensidadeSol");
        locEnergia = gl.glGetUniformLocation(programa, "energiaAbsorvida");
        locSurgimento = gl.glGetUniformLocation(programa, "surgimento");
        locIntensidade = gl.glGetUniformLocation(programa, "intensidade");
        locPuxao = gl.glGetUniformLocation(programa, "puxao");
        locAlvoPuxao = gl.glGetUniformLocation(programa, "alvoPuxao");
        locCentroPuxao = gl.glGetUniformLocation(programa, "centroPuxao");

        montarTrajetos();
        inicioNanos = System.nanoTime();
    }

    private int enviar(GL4 gl, int vao, int vbo, float[] dados) {
        if (dados.length == 0) return 0;
        FloatBuffer buffer = Buffers.newDirectFloatBuffer(dados);
        gl.glBindVertexArray(vao);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vbo);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, buffer.limit() * 4L, buffer, GL4.GL_STATIC_DRAW);
        return dados.length / 9;
    }

    /**
     * Separa os triângulos da mandíbula (gengiva de baixo e dentes de baixo) do resto da criatura,
     * para poder abrir a boca. Usa só os dados que o OBJLoader devolve: posição e cor de cada vértice.
     * A mandíbula fica na frente da cabeça (z > 11,4), abaixo da boca (y < 0,4) e tem cor de gengiva
     * (vermelho maior que verde) ou de dente (claro); a pele ao redor é verde-azulada escura.
     * @return [0] = corpo, [1] = mandíbula
     */
    private static float[][] separarMandibula(float[] dados) {
        List<Float> corpo = new ArrayList<>(), mandibula = new ArrayList<>();
        for (int tri = 0; tri < dados.length / 27; tri++) {
            boolean ehMandibula = true;
            for (int v = 0; v < 3; v++) {
                int b = tri * 27 + v * 9;
                float y = dados[b + 1], z = dados[b + 2], r = dados[b + 3], g = dados[b + 4];
                boolean corDeBoca = r > g || r > 0.5f;
                if (z < 11.4f || y > 0.4f || !corDeBoca) {
                    ehMandibula = false;
                    break;
                }
            }
            List<Float> destino = ehMandibula ? mandibula : corpo;
            for (int k = 0; k < 27; k++) destino.add(dados[tri * 27 + k]);
        }
        return new float[][]{paraArray(corpo), paraArray(mandibula)};
    }

    /** Move a malha para que o centro da sua caixa envolvente fique na origem. */
    private static void centralizarPelaCaixa(float[] dados) {
        float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (int i = 0; i < dados.length; i += 9) {
            for (int e = 0; e < 3; e++) {
                min[e] = Math.min(min[e], dados[i + e]);
                max[e] = Math.max(max[e], dados[i + e]);
            }
        }
        for (int i = 0; i < dados.length; i += 9) {
            for (int e = 0; e < 3; e++) dados[i + e] -= (min[e] + max[e]) * 0.5f;
        }
    }

    /** Calcula o voo da nave, a aproximação da criatura e a fuga. */
    private void montarTrajetos() {
        Vector3f pAneis = planetaAneis.posicao(T_PLANETA_ANEIS, new Vector3f());
        Vector3f pTerra = planetaTerra.posicao(T_PLANETA_TERRA, new Vector3f());
        Vector3f pLava = planetaLava.posicao(T_PLANETA_LAVA, new Vector3f());
        Vector3f radialLava = new Vector3f(pLava).sub(CENTRO_SOL).normalize();
        Vector3f radialTerra = new Vector3f(pTerra).sub(CENTRO_SOL).normalize();

        List<Float> tempos = new ArrayList<>();
        List<Vector3f> pontos = new ArrayList<>();
        adicionar(tempos, pontos, T_SAI_HIPERESPACO, new Vector3f(BURACO_CHEGADA));                                      // sai do buraco
        adicionar(tempos, pontos, T_PLANETA_ANEIS, new Vector3f(pAneis).add(0.0f, 9.0f, 24.0f));                       // sobre a borda dos anéis
        adicionar(tempos, pontos, T_PLANETA_LAVA, new Vector3f(pLava).fma(11.0f, radialLava).add(0.0f, 3.0f, 0.0f));   // rasante no vulcânico
        Vector3f depoisTerra = new Vector3f(pTerra).fma(22.0f, radialTerra).add(0.0f, 5.0f, 0.0f);                     // por fora da lua
        adicionar(tempos, pontos, T_PLANETA_TERRA, depoisTerra);

        // Curva final: arco de círculo para a esquerda, freando, até o bico apontar para o Sol
        Vector3f anterior = pontos.get(pontos.size() - 2);
        float velocidade = depoisTerra.distance(anterior) / (T_PLANETA_TERRA - T_PLANETA_LAVA);
        Vector3f rumo = new Vector3f(depoisTerra).sub(anterior);
        rumo.y = 0.0f;
        rumo.normalize();
        Vector3f esquerda = new Vector3f(rumo.z, 0.0f, -rumo.x);
        Vector3f centroCurva = new Vector3f(depoisTerra).fma(RAIO_CURVA_FINAL, esquerda);

        float anguloTotal = (float) Math.PI;
        for (float graus = 30.0f; graus < 330.0f; graus += 0.25f) {
            float a = (float) Math.toRadians(graus);
            Vector3f p = girarNaCurva(depoisTerra, centroCurva, a);
            Vector3f direcao = girarNaCurva(new Vector3f(rumo), new Vector3f(), a);
            Vector3f paraSol = new Vector3f(CENTRO_SOL).sub(p);
            paraSol.y = 0.0f;
            if (direcao.dot(paraSol.normalize()) > 0.9995f) {
                anguloTotal = a;
                break;
            }
        }

        // Fração do arco percorrida (0..1): começa na velocidade de chegada e termina parada
        float duracao = T_PARADA - T_PLANETA_TERRA;
        float comprimento = RAIO_CURVA_FINAL * anguloTotal;
        float c = limitar(velocidade * duracao / comprimento, 0.0f, 3.0f);
        int amostras = 10;
        for (int i = 1; i <= amostras; i++) {
            float x = (float) i / amostras;
            float fracao = c * x + (3.0f - 2.0f * c) * x * x + (c - 2.0f) * x * x * x;
            Vector3f p = girarNaCurva(depoisTerra, centroCurva, anguloTotal * fracao);
            p.y = depoisTerra.y + (ALTURA_PARADA - depoisTerra.y) * fracao;
            adicionar(tempos, pontos, T_PLANETA_TERRA + duracao * x, p);
        }
        pontoParada = new Vector3f(pontos.get(pontos.size() - 1));

        float[] arrayTempos = new float[tempos.size()];
        for (int i = 0; i < arrayTempos.length; i++) arrayTempos[i] = tempos.get(i);
        trajetoNave = new Trajeto(arrayTempos, pontos.toArray(new Vector3f[0]), true);

        // Eixos da vista da nave parada (frente = em direção ao Sol)
        frenteParada = new Vector3f(CENTRO_SOL).sub(pontoParada).normalize();
        direitaParada = new Vector3f(frenteParada).cross(CIMA).normalize();
        cimaParada = new Vector3f(direitaParada).cross(frenteParada).normalize();
        direcaoSaidaChegada = trajetoNave.direcao(T_SAI_HIPERESPACO + 0.3f, new Vector3f(), frenteParada);

        // A criatura se forma longe, atrás do Sol, e para com a boca ao lado dele
        Vector3f bocaFinal = ponto(CENTRO_SOL, RAIO_SOL + 9.0f, 0.0f, 1.5f);
        trajetoCriatura = new Trajeto(
            new float[]{T_CRIATURA_SURGE, 47.0f, 50.5f, T_CRIATURA_CHEGA},
            new Vector3f[]{
                ponto(CENTRO_SOL, 100.0f, 300.0f, -28.0f),
                ponto(CENTRO_SOL, 62.0f, 65.0f, -6.0f),
                ponto(CENTRO_SOL, 40.0f, 12.0f, 0.0f),
                bocaFinal
            }, true);
        direcaoComer = new Vector3f(direitaParada).mul(-1.0f).fma(-0.3f, frenteParada).normalize();

        // Fuga: meia-volta pela esquerda e mergulho num buraco aberto atrás da nave, longe da criatura
        Vector3f inicioFuga = new Vector3f(), dirInicio = new Vector3f();
        estadoNaveNoSistema(T_NAVE_FOGE, inicioFuga, dirInicio);
        Vector3f direcaoFuga = new Vector3f(frenteParada).mul(-0.8f).fma(-0.6f, direitaParada).fma(0.15f, CIMA).normalize();
        buracoSaida = new Vector3f(inicioFuga).fma(55.0f, direcaoFuga);
        trajetoFuga = new Trajeto(
            new float[]{T_NAVE_FOGE, T_NAVE_FOGE + 1.8f, T_ENTRA_HIPERESPACO, T_ENTRA_HIPERESPACO + 2.5f},
            new Vector3f[]{
                inicioFuga,
                new Vector3f(inicioFuga).fma(-3.5f, direitaParada).fma(-1.5f, frenteParada).fma(1.2f, CIMA),
                new Vector3f(buracoSaida),
                new Vector3f(buracoSaida).fma(40.0f, direcaoFuga)
            }, true, false);
    }

    private static void adicionar(List<Float> tempos, List<Vector3f> pontos, float t, Vector3f p) {
        tempos.add(t);
        pontos.add(p);
    }

    /** Gira 'p' em torno de 'centro' no plano horizontal, no sentido da curva para a esquerda. */
    private static Vector3f girarNaCurva(Vector3f p, Vector3f centro, float angulo) {
        float c = (float) Math.cos(-angulo), s = (float) Math.sin(-angulo);
        float vx = p.x - centro.x, vz = p.z - centro.z;
        return new Vector3f(centro.x + vx * c - vz * s, p.y, centro.z + vx * s + vz * c);
    }

    /** Ponto = base + direita*d + frente*f + cima*c (eixos da vista da nave parada). */
    private Vector3f ponto(Vector3f base, float d, float f, float c) {
        return new Vector3f(base).fma(d, direitaParada).fma(f, frenteParada).fma(c, cimaParada);
    }

    // =====================================================================
    //                              QUADRO A QUADRO
    // =====================================================================

    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();
        float tAbsoluto = tempoInicial + (System.nanoTime() - inicioNanos) / 1_000_000_000.0f;

        desenharQuadro(gl, Math.max(1, drawable.getSurfaceWidth()), Math.max(1, drawable.getSurfaceHeight()), tAbsoluto);

        if (!REPETIR_EM_LOOP && tAbsoluto >= PERIODO && !terminou) {
            terminou = true;
            if (aoTerminar != null) aoTerminar.run();
        }
    }

    private void desenharQuadro(GL4 gl, int largura, int altura, float tAbsoluto) {
        // Tempo dentro do ciclo: ao chegar em PERIODO volta a zero
        float t = REPETIR_EM_LOOP ? tAbsoluto % PERIODO : Math.min(tAbsoluto, PERIODO);

        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glDisable(GL4.GL_CULL_FACE);
        gl.glDisable(GL4.GL_BLEND);
        gl.glDepthMask(true);
        gl.glClearColor(0.004f, 0.006f, 0.012f, 1.0f);
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);
        gl.glUseProgram(programa);

        // ---------- Nave ----------
        Vector3f posNave = new Vector3f(), dirNave = new Vector3f();
        float rolagem = estadoNave(t, posNave, dirNave);
        boolean naveVisivel = t >= T_SAI_HIPERESPACO && t < T_ENTRA_HIPERESPACO + 0.03f;
        Matrix4f modeloNave = new Matrix4f()
            .translate(posNave)
            .rotateTowards(new Vector3f(dirNave).mul(-1.0f), CIMA)   // o bico da nave é o -Z do modelo
            .rotateZ(rolagem)
            .scale(ESCALA_NAVE)
            .translate(0.0f, -0.7f, 1.4f);                            // centraliza o modelo

        // ---------- Criatura ----------
        float surgimento = limitar((t - T_CRIATURA_SURGE) / DURACAO_SURGIMENTO, 0.0f, 1.0f);
        boolean criaturaVisivel = surgimento > 0.0f;
        Vector3f cabeca = new Vector3f(), frenteCriatura = new Vector3f();
        estadoCriatura(t, cabeca, frenteCriatura);
        Matrix4f modeloCriatura = new Matrix4f()
            .translate(cabeca)
            .rotateTowards(frenteCriatura, CIMA)
            .rotateY((float) Math.sin(t * 0.55f) * 0.06f)   // ondula como se nadasse
            .rotateX((float) Math.sin(t * 0.40f) * 0.04f)
            .scale(ESCALA_CRIATURA)
            .translate(-BOCA_LOCAL.x, -BOCA_LOCAL.y, -BOCA_LOCAL.z);

        // Boca: abre para devorar o Sol e de novo no bote contra a nave
        float mordidaSol = suave(T_BOCA_ABRE, T_BOCA_ABRE + 1.8f, t) * (1.0f - suave(T_BOCA_FECHA - 0.35f, T_BOCA_FECHA, t));
        float mordidaNave = suave(T_BOTE + 0.5f, T_BOTE + 2.0f, t) * (1.0f - suave(T_ENTRA_HIPERESPACO - 0.3f, T_ENTRA_HIPERESPACO + 0.2f, t));
        float aberturaBoca = (float) Math.toRadians(40.0f) * Math.max(mordidaSol, mordidaNave);
        Matrix4f modeloMandibula = new Matrix4f(modeloCriatura)
            .translate(DOBRADICA_MANDIBULA)
            .rotateX(aberturaBoca)
            .translate(-DOBRADICA_MANDIBULA.x, -DOBRADICA_MANDIBULA.y, -DOBRADICA_MANDIBULA.z);
        Vector3f boca = modeloCriatura.transformPosition(new Vector3f(0.0f, 0.3f, 12.6f));

        // ---------- Sol: é sugado para dentro da boca, encolhe, avermelha e apaga ----------
        float u = limitar((t - T_DEVORAR_INICIO) / (T_DEVORAR_FIM - T_DEVORAR_INICIO), 0.0f, 1.0f);
        float avanco = (float) Math.pow(u, 1.7);
        boolean solVisivel = t < T_DEVORAR_FIM;
        Vector3f centroSol = new Vector3f(CENTRO_SOL).lerp(boca, avanco * 0.92f);
        float escalaSol = ESCALA_SOL * (1.0f - 0.95f * avanco);
        float raioSolAtual = RAIO_SOL * escalaSol / ESCALA_SOL;
        float puxao = 0.22f * suave(T_BOCA_ABRE + 0.5f, T_DEVORAR_INICIO, t)
                    + 0.78f * suave(T_DEVORAR_INICIO, T_DEVORAR_INICIO + 1.2f, t);
        float intensidadeSol = 1.0f - suave(T_DEVORAR_INICIO + 0.8f, T_DEVORAR_FIM + 0.1f, t);
        if (u > 0.0f && u < 1.0f) intensidadeSol *= 0.88f + 0.12f * (float) Math.sin(t * 41.0f); // agonia
        Vector3f corSol = new Vector3f(COR_SOL).lerp(COR_SOL_MORRENDO, suave(T_BOCA_ABRE, T_DEVORAR_FIM - 0.8f, t));

        float energia = t < T_BOCA_FECHA
            ? 0.4f * suave(T_DEVORAR_INICIO, T_BOCA_FECHA, t)
            : 0.15f + 0.85f * (float) Math.exp(-(t - T_BOCA_FECHA) / 2.2f);

        // ---------- Farol da nave mira a cabeça da criatura ----------
        float farol = intensidadeFarol(t);
        Vector3f posFarol = modeloNave.transformPosition(new Vector3f(0.0f, 0.2f, -7.2f));
        Vector3f dirFarol = new Vector3f(cabeca).fma(-4.0f, cimaParada).sub(posFarol).normalize();

        // ---------- Buracos de hiperespaço ----------
        // Chegada: aberto no começo, clarão quando a nave sai, depois se fecha
        float buracoChegada = 1.0f - suave(T_BURACO_FECHA, T_BURACO_FECHA + 1.2f, t);
        float claraoChegada = (float) Math.exp(-Math.abs(t - T_SAI_HIPERESPACO) / 0.25f);
        // Saída: abre durante a fuga, clarão quando a nave entra
        float buracoSaidaAbertura = crescerComSobra(limitar((t - T_BURACO_SAIDA_ABRE) / 1.2f, 0.0f, 1.0f));
        float claraoSaida = (float) Math.exp(-Math.abs(t - T_ENTRA_HIPERESPACO) / 0.25f);
        // Túnel do hiperespaço cobrindo a tela: total no começo e no fim do ciclo (emenda do loop)
        float hiperespaco = Math.max(1.0f - suave(0.5f, T_SAI_HIPERESPACO, t), suave(T_ENTRA_HIPERESPACO - 0.1f, T_ENTRA_HIPERESPACO + 0.5f, t));

        // ---------- Câmera ----------
        Vector3f posCamera = new Vector3f(), alvoCamera = new Vector3f();
        calcularCamera(t, posCamera, alvoCamera);
        float aspecto = (float) largura / altura;
        Matrix4f projecao = new Matrix4f().setPerspective((float) Math.toRadians(45.0f), aspecto, 0.05f, 6000.0f);
        Matrix4f visualizacao = new Matrix4f().setLookAt(
            posCamera.x, posCamera.y, posCamera.z,
            alvoCamera.x, alvoCamera.y, alvoCamera.z,
            0.0f, 1.0f, 0.0f);
        Matrix4f vp = new Matrix4f();
        projecao.mul(visualizacao, vp);

        // ---------- Uniforms do quadro ----------
        vp.get(arrayMatriz);
        gl.glUniformMatrix4fv(locVP, 1, false, arrayMatriz, 0);
        gl.glUniform3f(locPosCamera, posCamera.x, posCamera.y, posCamera.z);
        gl.glUniform1f(locTempo, t);
        gl.glUniform1f(locPeriodo, PERIODO);
        gl.glUniform1f(locFade, 1.0f);
        gl.glUniform1f(locAspecto, aspecto);
        gl.glUniform3f(locPosSol, centroSol.x, centroSol.y, centroSol.z);
        gl.glUniform3f(locCorSol, corSol.x, corSol.y, corSol.z);
        gl.glUniform1f(locIntensidadeSol, solVisivel ? intensidadeSol : 0.0f);
        gl.glUniform3f(locPosLuzNave, posFarol.x, posFarol.y, posFarol.z);
        gl.glUniform3f(locDirLuzNave, dirFarol.x, dirFarol.y, dirFarol.z);
        gl.glUniform1f(locIntensidadeFarol, farol);
        gl.glUniform1f(locEnergia, energia);
        gl.glUniform1f(locSurgimento, surgimento);
        gl.glUniform1f(locPuxao, 0.0f);
        gl.glUniform1f(locIntensidade, 0.0f);

        // ---------- Estrelas (sempre "no infinito": acompanham a câmera) ----------
        gl.glUniform1i(locModo, 0);
        enviarModelo(gl, new Matrix4f().translate(posCamera));
        gl.glPointSize(2.0f);
        gl.glBindVertexArray(vaoEstrelas);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vboEstrelas);
        configurarAtributos(gl);
        gl.glDrawArrays(GL4.GL_POINTS, 0, QTD_ESTRELAS);

        // ---------- Planetas ----------
        gl.glUniform1i(locModo, 1);
        Matrix4f modeloPlaneta = new Matrix4f();
        for (Planeta p : new Planeta[]{planetaLava, planetaTerra, planetaAneis}) {
            if (p.qtd > 0) desenhar(gl, p.vao, p.vbo, p.qtd, p.modelo(t, modeloPlaneta));
        }

        // ---------- Sol ----------
        if (solVisivel && qtdSol > 0) {
            gl.glUniform1i(locModo, 5);
            gl.glUniform1f(locIntensidade, 1.0f + 0.35f * puxao);
            gl.glUniform1f(locPuxao, puxao);
            gl.glUniform3f(locAlvoPuxao, boca.x, boca.y, boca.z);
            gl.glUniform3f(locCentroPuxao, centroSol.x, centroSol.y, centroSol.z);
            desenhar(gl, vaoSol, vboSol, qtdSol, new Matrix4f().translate(centroSol).rotateY(t * 0.05f).scale(escalaSol));
            gl.glUniform1f(locPuxao, 0.0f);

            // Coroa do Sol: antes da nave e da criatura, para que elas fiquem por cima do brilho
            ligarMistura(gl, true, true);
            gl.glUniform1i(locModo, 4);
            gl.glUniform1f(locIntensidade, 0.55f + 0.45f * intensidadeSol);
            desenharHalo(gl, centroSol, raioSolAtual * 1.02f, raioSolAtual * 3.4f, posCamera);
            ligarMistura(gl, false, false);
        }

        // ---------- Nave e criatura ----------
        if (naveVisivel) {
            gl.glUniform1i(locModo, 1);
            gl.glUniform1f(locIntensidade, 0.22f);
            desenhar(gl, vaoNave, vboNave, qtdNave, modeloNave);
        }
        if (criaturaVisivel && qtdCriatura > 0) {
            gl.glUniform1i(locModo, 2);
            desenhar(gl, vaoCriatura, vboCriatura, qtdCriatura, modeloCriatura);
            if (qtdMandibula > 0) desenhar(gl, vaoMandibula, vboMandibula, qtdMandibula, modeloMandibula);
        }

        // ---------- Efeitos de luz somada ----------
        ligarMistura(gl, true, true);
        gl.glUniform1i(locModo, 4);
        float brilhoBoca = suave(T_DEVORAR_INICIO, T_DEVORAR_INICIO + 1.0f, t) * (1.0f - suave(T_BOCA_FECHA - 0.4f, T_BOCA_FECHA, t));
        if (criaturaVisivel && brilhoBoca > 0.01f) {
            gl.glUniform1f(locIntensidade, brilhoBoca * (0.8f + 0.2f * (float) Math.sin(t * 23.0f)));
            desenharHalo(gl, boca, 1.0f, 14.0f, posCamera);
        }
        if (farol > 0.01f) {
            gl.glUniform1i(locModo, 3);
            float escalaFeixe = 2.2f;
            gl.glUniform1f(locAlcanceFeixe, 24.0f * escalaFeixe);
            Matrix4f modeloFeixe = new Matrix4f()
                .translate(posFarol)
                .rotateTowards(new Vector3f(dirFarol).mul(-1.0f), CIMA)
                .scale(escalaFeixe * 0.55f, escalaFeixe * 0.55f, escalaFeixe)
                .translate(0.0f, 0.0f, 7.0f); // a ponta do cone gerado fica em z = -7
            desenhar(gl, vaoFeixe, vboFeixe, qtdFeixe, modeloFeixe);
        }
        ligarMistura(gl, false, false);

        // ---------- Buracos de hiperespaço (disco escuro com redemoinho e borda brilhante) ----------
        ligarMistura(gl, true, false);
        gl.glUniform1i(locModo, 6);
        if (buracoChegada > 0.01f) {
            gl.glUniform1f(locIntensidade, 1.0f + 1.5f * claraoChegada);
            desenharHalo(gl, BURACO_CHEGADA, 0.0f, RAIO_BURACO * buracoChegada * (1.0f + 0.25f * claraoChegada), posCamera);
        }
        if (buracoSaidaAbertura > 0.01f) {
            gl.glUniform1f(locIntensidade, 1.0f + 1.5f * claraoSaida);
            desenharHalo(gl, buracoSaida, 0.0f, RAIO_BURACO * buracoSaidaAbertura * (1.0f + 0.25f * claraoSaida), posCamera);
        }

        // ---------- Túnel do hiperespaço na tela inteira ----------
        if (hiperespaco > 0.0f) {
            gl.glDisable(GL4.GL_DEPTH_TEST);
            gl.glUniform1i(locModo, 7);
            gl.glUniform1f(locIntensidade, hiperespaco);
            desenhar(gl, vaoQuad, vboQuad, qtdQuad, new Matrix4f());
            gl.glEnable(GL4.GL_DEPTH_TEST);
        }
        ligarMistura(gl, false, false);
    }

    /** Liga/desliga a transparência (aditiva para brilhos, normal para os buracos). Não grava profundidade. */
    private void ligarMistura(GL4 gl, boolean ligar, boolean aditiva) {
        if (ligar) {
            gl.glEnable(GL4.GL_BLEND);
            if (aditiva) {
                gl.glBlendFuncSeparate(GL4.GL_SRC_ALPHA, GL4.GL_ONE, GL4.GL_ZERO, GL4.GL_ONE);
            } else {
                gl.glBlendFuncSeparate(GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA, GL4.GL_ZERO, GL4.GL_ONE);
            }
            gl.glDepthMask(false);
        } else {
            gl.glDepthMask(true);
            gl.glDisable(GL4.GL_BLEND);
        }
    }

    // =====================================================================
    //                              PERSONAGENS
    // =====================================================================

    /** Posição e direção da nave no instante t. Retorna a inclinação lateral (rolagem) nas curvas. */
    private float estadoNave(float t, Vector3f pos, Vector3f dir) {
        if (t < T_NAVE_FOGE) return estadoNaveNoSistema(t, pos, dir);

        // Fuga: parte do jeito que estava e gira aos poucos para o rumo do buraco
        Vector3f dirParada = new Vector3f();
        estadoNaveNoSistema(T_NAVE_FOGE, new Vector3f(), dirParada);
        trajetoFuga.posicao(t, pos);
        float tMov = Math.max(t, T_NAVE_FOGE + 0.6f);
        Vector3f movimento = trajetoFuga.direcao(tMov, new Vector3f(), dirParada);
        float w = suave(T_NAVE_FOGE, T_NAVE_FOGE + 1.8f, t);
        dir.set(dirParada).lerp(movimento, w).normalize();

        Vector3f antes = trajetoFuga.direcao(tMov - 0.5f, new Vector3f(), dirParada);
        Vector3f depois = trajetoFuga.direcao(tMov + 0.5f, new Vector3f(), dirParada);
        return limitar(new Vector3f(antes).cross(depois).y, -0.7f, 0.7f) * w;
    }

    private float estadoNaveNoSistema(float t, Vector3f pos, Vector3f dir) {
        trajetoNave.posicao(t, pos);

        // Direção de voo (perto da parada usa a direção de um pouco antes, quando ainda havia velocidade)
        float tVoo = limitar(t, T_SAI_HIPERESPACO + 0.3f, T_PARADA - 0.35f);
        trajetoNave.direcao(tVoo, dir, frenteParada);

        // Inclina para dentro das curvas
        Vector3f antes = trajetoNave.direcao(tVoo - 0.6f, new Vector3f(), frenteParada);
        Vector3f depois = trajetoNave.direcao(tVoo + 0.6f, new Vector3f(), frenteParada);
        float giro = new Vector3f(antes).cross(depois).y;

        // Ao parar, vira de frente para o Sol e fica flutuando
        float parado = suave(T_PARADA - 4.0f, T_PARADA, t);
        dir.lerp(frenteParada, parado).normalize();
        pos.y += (float) Math.sin(t * 1.4f) * 0.05f * parado;

        // Quando o farol liga, vira um pouco para encarar a criatura
        if (t > T_FAROL_LIGA - 1.0f) {
            Vector3f cabeca = new Vector3f(), frente = new Vector3f();
            estadoCriatura(t, cabeca, frente);
            Vector3f paraCriatura = new Vector3f(cabeca).sub(pos).normalize();
            dir.lerp(paraCriatura, 0.3f * suave(T_FAROL_LIGA - 1.0f, T_FAROL_LIGA + 2.5f, t)).normalize();
        }

        // Recua, assustada, enquanto o Sol é devorado
        pos.fma(-2.0f * suave(T_BOCA_ABRE, T_DEVORAR_FIM + 1.0f, t), frenteParada);

        return limitar(giro, -0.7f, 0.7f) * (1.0f - parado);
    }

    /** Posição da boca e direção da cabeça da criatura no instante t. */
    private void estadoCriatura(float t, Vector3f cabeca, Vector3f frente) {
        trajetoCriatura.posicao(t, cabeca);
        float tMov = limitar(t, trajetoCriatura.inicio(), T_CRIATURA_CHEGA - 0.3f);
        trajetoCriatura.direcao(tMov, frente, direcaoComer);

        // Ao chegar, vira a boca para o Sol
        frente.lerp(direcaoComer, suave(T_CRIATURA_CHEGA - 3.0f, T_CRIATURA_CHEGA, t)).normalize();

        // Depois de devorar, encara a nave e se aproxima devagar
        float olhar = suave(T_BOCA_FECHA + 0.8f, T_NAVE_FOGE, t);
        if (olhar > 0.0f) {
            Vector3f paraNave = new Vector3f(pontoParada).sub(cabeca).normalize();
            cabeca.fma(8.0f * suave(T_BOCA_FECHA + 1.0f, T_NAVE_FOGE + 1.0f, t), paraNave);
            frente.lerp(paraNave, 0.8f * olhar).normalize();
        }

        // Bote: persegue o rastro da nave em fuga, quase alcançando
        float bote = suave(T_BOTE, T_ENTRA_HIPERESPACO + 0.5f, t);
        if (bote > 0.0f) {
            Vector3f rastro = trajetoFuga.posicao(t - 1.1f, new Vector3f());
            cabeca.lerp(rastro, 0.85f * bote);
            Vector3f paraRastro = new Vector3f(trajetoFuga.posicao(t, new Vector3f())).sub(cabeca);
            if (paraRastro.length() > 1e-3f) frente.lerp(paraRastro.normalize(), bote).normalize();
        }
    }

    private float intensidadeFarol(float t) {
        if (t < T_FAROL_LIGA) return 0.0f;
        if (t < T_FAROL_LIGA + 0.9f) return ruido(t * 18.0f) > 0.45f ? 1.0f : 0.1f;  // liga falhando
        if (t < T_FAROL_FALHA) return 1.0f;
        if (t < T_FAROL_FALHA + 1.4f) return ruido(t * 15.0f) > 0.55f ? 0.8f : 0.0f; // pisca e morre
        return 0.0f;
    }

    // =====================================================================
    //                                CÂMERA
    // =====================================================================

    private void calcularCamera(float t, Vector3f pos, Vector3f alvo) {
        int atual = 0;
        while (atual + 1 < TOMADA_INICIO.length && t >= TOMADA_INICIO[atual + 1]) atual++;

        tomada(atual, t, pos, alvo);
        float trans = TOMADA_TRANSICAO[atual];
        if (atual > 0 && trans > 0.0f && t < TOMADA_INICIO[atual] + trans) {
            Vector3f posAnterior = new Vector3f(), alvoAnterior = new Vector3f();
            tomada(atual - 1, t, posAnterior, alvoAnterior);
            float w = suave(TOMADA_INICIO[atual], TOMADA_INICIO[atual] + trans, t);
            pos.set(posAnterior.lerp(pos, w));
            alvo.set(alvoAnterior.lerp(alvo, w));
        }
    }

    private void tomada(int indice, float t, Vector3f pos, Vector3f alvo) {
        switch (indice) {
            case 0: { // Chegada: câmera diante do buraco; a nave sai dele e passa rente
                Vector3f lado = new Vector3f(direcaoSaidaChegada).cross(CIMA).normalize();
                pos.set(BURACO_CHEGADA).fma(16.0f, direcaoSaidaChegada).fma(3.0f, lado).fma(1.2f, CIMA);
                Vector3f nave = trajetoNave.posicao(t, new Vector3f());
                alvo.set(BURACO_CHEGADA).lerp(nave, suave(T_SAI_HIPERESPACO + 0.2f, T_SAI_HIPERESPACO + 1.4f, t) * 0.9f);
                break;
            }

            case 1: // Perseguição: atrás e um pouco acima da nave
                perseguicao(t, 6.0f, 1.8f, 1.0f, pos, alvo);
                break;

            case 2: // Perseguição baixa, rasante pelo planeta vulcânico
                perseguicao(t, 5.0f, 0.9f, -1.6f, pos, alvo);
                break;

            case 3: { // Câmera parada à beira do caminho: a nave vem do planeta com lua e passa rente a ela
                Vector3f passagem = trajetoNave.posicao(T_PLANETA_TERRA, new Vector3f());
                Vector3f radial = new Vector3f(passagem).sub(CENTRO_SOL);
                radial.y = 0.0f;
                radial.normalize();
                Vector3f rumo = trajetoNave.direcao(T_PLANETA_TERRA, new Vector3f(), frenteParada);
                pos.set(passagem).fma(4.0f, rumo).fma(2.0f, radial).add(0.0f, 0.8f, 0.0f);
                Vector3f nave = trajetoNave.posicao(t, new Vector3f());
                alvo.set(planetaTerra.posicao(t, new Vector3f())).lerp(nave, 0.75f);
                break;
            }

            case 4: // Mais aberta durante a curva
                perseguicao(t, 7.0f, 2.6f, 0.0f, pos, alvo);
                break;

            case 5: { // Contemplação: atrás da nave, o Sol enorme à frente
                float aproxima = suave(39.0f, 46.0f, t);
                pos.set(pontoParada).fma(-(3.0f - 0.6f * aproxima), frenteParada).fma(0.8f, cimaParada).fma(0.65f, direitaParada);
                alvo.set(CENTRO_SOL).fma(-5.0f, direitaParada).fma(-5.0f, cimaParada);
                break;
            }

            case 6: // Abre o quadro: nave minúscula, Sol e a criatura chegando
                pos.set(pontoParada).fma(-40.0f, frenteParada).fma(12.0f, cimaParada).fma(-13.0f, direitaParada);
                alvo.set(CENTRO_SOL).fma(18.0f, direitaParada).fma(1.0f, cimaParada);
                break;

            case 7: { // Escuridão: aproxima devagar da cabeça da criatura
                tomada(6, t, pos, alvo);
                Vector3f cabeca = new Vector3f(), frente = new Vector3f();
                estadoCriatura(t, cabeca, frente);
                alvo.lerp(cabeca, 0.8f);
                pos.lerp(cabeca, 0.25f * suave(TOMADA_INICIO[7], T_NAVE_FOGE + 1.0f, t));
                break;
            }

            case 8: { // Fuga: câmera do outro lado da nave, olhando para a criatura que vem atrás dela
                Vector3f nave = new Vector3f();
                estadoNave(t, nave, new Vector3f());
                Vector3f cabeca = new Vector3f(), frente = new Vector3f();
                estadoCriatura(t, cabeca, frente);
                Vector3f paraCriatura = new Vector3f(cabeca).sub(nave).normalize();
                Vector3f lado = new Vector3f(paraCriatura).cross(CIMA).normalize();
                pos.set(nave).fma(-2.4f, paraCriatura).fma(0.6f, lado).fma(0.5f, CIMA);
                alvo.set(nave).lerp(cabeca, 0.15f);
                break;
            }

            default: { // Mergulho: atrás da nave, entrando no buraco junto com ela
                Vector3f p = trajetoFuga.posicao(t - 0.15f, new Vector3f());
                Vector3f d = trajetoFuga.direcao(t - 0.15f, new Vector3f(), frenteParada);
                pos.set(p).fma(-2.2f, d).fma(0.6f, CIMA);
                alvo.set(buracoSaida);
                break;
            }
        }
    }

    /** Câmera que segue a nave com um pequeno atraso (dá a sensação de inércia nas curvas). */
    private void perseguicao(float t, float atras, float acima, float lado, Vector3f pos, Vector3f alvo) {
        Vector3f p = new Vector3f(), d = new Vector3f();
        estadoNave(t - 0.12f, p, new Vector3f());
        estadoNave(t - 0.5f, new Vector3f(), d);
        Vector3f direita = new Vector3f(d).cross(CIMA).normalize();
        pos.set(p).fma(-atras, d).fma(acima, CIMA).fma(lado, direita);

        Vector3f pAgora = new Vector3f(), dAgora = new Vector3f();
        estadoNave(t, pAgora, dAgora);
        alvo.set(pAgora).fma(5.0f, dAgora);
    }

    // =====================================================================
    //                              DESENHO
    // =====================================================================

    private void enviarModelo(GL4 gl, Matrix4f modelo) {
        modelo.get(arrayMatriz);
        gl.glUniformMatrix4fv(locModelo, 1, false, arrayMatriz, 0);
    }

    private void configurarAtributos(GL4 gl) {
        // Todas as malhas usam 9 floats por vértice: posição, cor, normal
        int stride = 9 * 4;
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, stride, 0);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, stride, 3 * 4L);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(2, 3, GL4.GL_FLOAT, false, stride, 6 * 4L);
        gl.glEnableVertexAttribArray(2);
    }

    private void desenhar(GL4 gl, int vao, int vbo, int qtdVertices, Matrix4f modelo) {
        enviarModelo(gl, modelo);
        gl.glBindVertexArray(vao);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vbo);
        configurarAtributos(gl);
        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, qtdVertices);
    }

    /** Quadrado virado para a câmera, um pouco à frente de 'centro', com o raio dado. */
    private void desenharHalo(GL4 gl, Vector3f centro, float avancoParaCamera, float raio, Vector3f posCamera) {
        Vector3f paraCamera = new Vector3f(posCamera).sub(centro).normalize();
        Matrix4f modelo = new Matrix4f()
            .translate(new Vector3f(centro).fma(avancoParaCamera, paraCamera))
            .rotateTowards(paraCamera, CIMA)
            .scale(Math.max(raio, 1e-4f));
        desenhar(gl, vaoQuad, vboQuad, qtdQuad, modelo);
    }

    // =====================================================================
    //                         GEOMETRIA GERADA
    // =====================================================================

    private float[] gerarConeHolofote() {
        int fatias = 24;
        float comprimento = 24.0f;
        float raioFinal = 6.5f;
        List<Float> lista = new ArrayList<>();
        for (int i = 0; i < fatias; i++) {
            double ang1 = (2.0 * Math.PI * i) / fatias;
            double ang2 = (2.0 * Math.PI * (i + 1)) / fatias;
            float x1 = (float) Math.cos(ang1) * raioFinal, y1 = (float) Math.sin(ang1) * raioFinal;
            float x2 = (float) Math.cos(ang2) * raioFinal, y2 = (float) Math.sin(ang2) * raioFinal;
            adicionarVerticeSimples(lista, 0.0f, 0.0f, -7.0f, 0.75f, 0.95f, 1.0f);
            adicionarVerticeSimples(lista, x1, y1, -7.0f - comprimento, 0.35f, 0.75f, 0.90f);
            adicionarVerticeSimples(lista, x2, y2, -7.0f - comprimento, 0.35f, 0.75f, 0.90f);
        }
        return paraArray(lista);
    }

    /** Quadrado de -1 a 1 no plano XY (halos, buracos e o túnel em tela cheia). */
    private float[] gerarQuadrado() {
        List<Float> lista = new ArrayList<>();
        float[][] cantos = {{-1, -1}, {1, -1}, {1, 1}, {-1, -1}, {1, 1}, {-1, 1}};
        for (float[] c : cantos) adicionarVerticeSimples(lista, c[0], c[1], 0.0f, 1.0f, 1.0f, 1.0f);
        return paraArray(lista);
    }

    private void adicionarVerticeSimples(List<Float> lista, float x, float y, float z, float r, float g, float b) {
        lista.add(x); lista.add(y); lista.add(z);
        lista.add(r); lista.add(g); lista.add(b);
        lista.add(0.0f); lista.add(1.0f); lista.add(0.0f);
    }

    private float[] gerarEstrelas(int quantidade) {
        Random rand = new Random(42);
        float[] dados = new float[quantidade * 9];
        for (int i = 0; i < quantidade; i++) {
            double theta = rand.nextDouble() * 2.0 * Math.PI;
            double phi = Math.acos(2.0 * rand.nextDouble() - 1.0);
            double raio = 1500.0 + rand.nextDouble() * 1300.0;

            float brilho = 0.35f + rand.nextFloat() * 0.65f;
            float r = brilho, g = brilho, b = brilho;
            int tipoCor = rand.nextInt(8);
            if (tipoCor <= 2) {
                r *= 0.45f;
                g *= 0.90f;
            } else if (tipoCor == 3) {
                b *= 0.75f;
            }

            int base = i * 9;
            dados[base]     = (float) (raio * Math.sin(phi) * Math.cos(theta));
            dados[base + 1] = (float) (raio * Math.sin(phi) * Math.sin(theta));
            dados[base + 2] = (float) (raio * Math.cos(phi));
            dados[base + 3] = r;
            dados[base + 4] = g;
            dados[base + 5] = b;
        }
        return dados;
    }

    private static float[] paraArray(List<Float> lista) {
        float[] arr = new float[lista.size()];
        for (int i = 0; i < lista.size(); i++) arr[i] = lista.get(i);
        return arr;
    }

    // =====================================================================
    //                              UTILITÁRIOS
    // =====================================================================

    /** Cresce de 0 a 1 passando um pouco de 1 antes de assentar (abertura dos buracos). */
    private static float crescerComSobra(float x) {
        float c1 = 1.2f, c3 = c1 + 1.0f, y = x - 1.0f;
        return 1.0f + c3 * y * y * y + c1 * y * y;
    }

    /** 0 antes de 'a', 1 depois de 'b', transição suave no meio (smoothstep). */
    private static float suave(float a, float b, float t) {
        float x = limitar((t - a) / (b - a), 0.0f, 1.0f);
        return x * x * (3.0f - 2.0f * x);
    }

    private static float limitar(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    /** Número pseudoaleatório em [0,1) que muda a cada inteiro de x (para piscadas). */
    private static float ruido(float x) {
        double s = Math.sin(Math.floor(x) * 12.9898) * 43758.5453;
        return (float) (s - Math.floor(s));
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        drawable.getGL().getGL4().glViewport(x, y, width, height);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();
        gl.glDeleteVertexArrays(todosVaos.length, todosVaos, 0);
        gl.glDeleteBuffers(todosVbos.length, todosVbos, 0);
    }
}
