#version 400 core

layout (location = 0) in vec3 posicao;
layout (location = 1) in vec3 cor;
layout (location = 2) in vec3 normal;

uniform mat4 matrizVP;       // projeção * visualização (câmera)
uniform mat4 matrizModelo;   // posição/rotação/escala do objeto
uniform int usarIluminacao;

// Sucção do Sol pela criatura (só afeta o modo 5)
uniform float puxao;         // 0 = Sol redondo, 1 = esticado ao máximo em direção à boca
uniform vec3 alvoPuxao;      // boca da criatura (mundo)
uniform vec3 centroPuxao;    // centro atual do Sol (mundo)

out vec3 corVertice;
out vec3 normalVertice;
out vec3 posicaoLocal;
out vec3 posicaoMundo;

void main() {
    posicaoLocal = posicao;

    // Modo 7: quadrado que cobre a tela inteira (túnel do hiperespaço), sem câmera
    if (usarIluminacao == 7) {
        posicaoMundo = vec3(0.0);
        corVertice = cor;
        normalVertice = vec3(0.0);
        gl_Position = vec4(posicao.xy, 0.0, 1.0);
        return;
    }

    vec4 mundo = matrizModelo * vec4(posicao, 1.0);
    vec3 n = mat3(matrizModelo) * normal;

    if (usarIluminacao == 5) {
        // A malha do Sol é uma esfera centrada na origem: a própria posição serve de normal
        n = mat3(matrizModelo) * posicao;

        if (puxao > 0.0) {
            vec3 paraBoca = alvoPuxao - centroPuxao;
            float dist = length(paraBoca);
            if (dist > 0.001) {
                vec3 dirBoca = paraBoca / dist;
                vec3 rel = mundo.xyz - centroPuxao;
                float aoLongo = dot(rel, dirBoca);
                // 1 no lado virado para a boca, 0 no lado oposto
                float lado = dot(normalize(rel), dirBoca) * 0.5 + 0.5;
                float peso = pow(lado, 2.5) * puxao;

                // Estica o lado da frente até a boca, formando uma gota de plasma...
                mundo.xyz += dirBoca * max(dist - aoLongo, 0.0) * peso * 0.95;
                // ...e afina a gota lateralmente perto da boca
                vec3 lateral = rel - dirBoca * aoLongo;
                mundo.xyz -= lateral * peso * 0.7;
            }
        }
    }

    posicaoMundo = mundo.xyz;
    gl_Position = matrizVP * mundo;
    corVertice = cor;
    normalVertice = n;
}
