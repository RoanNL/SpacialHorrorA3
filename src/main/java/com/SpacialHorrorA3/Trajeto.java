package com.SpacialHorrorA3;

import org.joml.Vector3f;

/**
 * Caminho suave que passa por pontos-chave em instantes definidos.
 *
 * É uma spline de Hermite com tangentes no estilo Catmull-Rom: a curva passa
 * exatamente por cada ponto no seu instante e a velocidade muda sem trancos.
 * Usado para o voo da nave e para a aproximação da criatura.
 */
public class Trajeto {

    private final float[] tempos;
    private final Vector3f[] pontos;
    private final Vector3f[] tangentes;

    /**
     * @param tempos     instantes (em segundos, crescentes) de cada ponto-chave
     * @param pontos     posições no mundo de cada ponto-chave
     * @param pararNoFim se true, a velocidade chega a zero no último ponto (frenagem suave)
     */
    public Trajeto(float[] tempos, Vector3f[] pontos, boolean pararNoFim) {
        this(tempos, pontos, false, pararNoFim);
    }

    /**
     * @param partirParado se true, a velocidade começa em zero no primeiro ponto (arrancada suave)
     * @param pararNoFim   se true, a velocidade chega a zero no último ponto (frenagem suave)
     */
    public Trajeto(float[] tempos, Vector3f[] pontos, boolean partirParado, boolean pararNoFim) {
        if (tempos.length != pontos.length || tempos.length < 2) {
            throw new IllegalArgumentException("Trajeto precisa de pelo menos 2 pontos, com um tempo para cada.");
        }
        this.tempos = tempos.clone();
        this.pontos = new Vector3f[pontos.length];
        for (int i = 0; i < pontos.length; i++) {
            this.pontos[i] = new Vector3f(pontos[i]);
        }

        int n = pontos.length;
        tangentes = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            int a = Math.max(0, i - 1);
            int b = Math.min(n - 1, i + 1);
            tangentes[i] = new Vector3f(this.pontos[b]).sub(this.pontos[a]).mul(1.0f / (tempos[b] - tempos[a]));
        }
        if (partirParado) {
            tangentes[0].set(0.0f, 0.0f, 0.0f);
        }
        if (pararNoFim) {
            tangentes[n - 1].set(0.0f, 0.0f, 0.0f);
        }
    }

    public float inicio() {
        return tempos[0];
    }

    public float fim() {
        return tempos[tempos.length - 1];
    }

    /** Posição no instante t (antes do início segue em linha reta; depois do fim fica no último ponto). */
    public Vector3f posicao(float t, Vector3f destino) {
        int n = tempos.length;
        if (t <= tempos[0]) {
            return destino.set(pontos[0]).fma(t - tempos[0], tangentes[0]);
        }
        if (t >= tempos[n - 1]) {
            return destino.set(pontos[n - 1]);
        }

        int i = 0;
        while (t > tempos[i + 1]) {
            i++;
        }

        float h = tempos[i + 1] - tempos[i];
        float s = (t - tempos[i]) / h;
        float s2 = s * s;
        float s3 = s2 * s;

        float h00 = 2 * s3 - 3 * s2 + 1;
        float h10 = s3 - 2 * s2 + s;
        float h01 = -2 * s3 + 3 * s2;
        float h11 = s3 - s2;

        Vector3f p0 = pontos[i], p1 = pontos[i + 1];
        Vector3f m0 = tangentes[i], m1 = tangentes[i + 1];

        destino.x = h00 * p0.x + h10 * h * m0.x + h01 * p1.x + h11 * h * m1.x;
        destino.y = h00 * p0.y + h10 * h * m0.y + h01 * p1.y + h11 * h * m1.y;
        destino.z = h00 * p0.z + h10 * h * m0.z + h01 * p1.z + h11 * h * m1.z;
        return destino;
    }

    /** Direção do movimento (vetor unitário) em t. Se estiver parado, devolve 'reserva'. */
    public Vector3f direcao(float t, Vector3f destino, Vector3f reserva) {
        Vector3f a = posicao(t - 0.05f, new Vector3f());
        Vector3f b = posicao(t + 0.05f, new Vector3f());
        destino.set(b).sub(a);
        if (destino.length() < 1e-4f) {
            return destino.set(reserva).normalize();
        }
        return destino.normalize();
    }
}
