#version 400 core

in vec3 corVertice;
in vec3 normalVertice;
in vec3 posicaoLocal;
in vec3 posicaoMundo;

uniform int usarIluminacao;
uniform vec3 posicaoCamera;
uniform vec3 posicaoLuzNave;
uniform vec3 direcaoLuzNave;

out vec4 corSaida;

void main() {
    // Modo 0: Estrelas de fundo (sem sombra)
    if (usarIluminacao == 0) {
        corSaida = vec4(corVertice, 1.0);
        return;
    }

    // Modo 3: Feixe de luz volumétrico (holofote translúcido da nave)
    if (usarIluminacao == 3) {
        // Suaviza o feixe conforme se afasta da nave
        float dist = length(posicaoMundo - posicaoLuzNave);
        float fadeDist = clamp(1.0 - (dist / 26.0), 0.0, 1.0);
        corSaida = vec4(corVertice, fadeDist * 0.16);
        return;
    }

    // Se a normal for zero (ex: propulsor da nave ou olho bioluminescente puro)
    if (length(normalVertice) < 0.1) {
        corSaida = vec4(corVertice, 1.0);
        return;
    }

    vec3 norm = normalize(normalVertice);
    vec3 dirVisao = normalize(posicaoCamera - posicaoMundo);

    // Modo 1: Iluminação da Nave
    if (usarIluminacao == 1) {
        vec3 direcaoLuz = normalize(vec3(-0.4, 0.6, 0.8));
        float difusa = max(dot(norm, direcaoLuz), 0.0);
        vec3 corFinal = corVertice * (0.20 + difusa * 0.85);
        corSaida = vec4(corFinal, 1.0);
        return;
    }

    // Modo 2: Shader da Criatura
    // Cálculo do holofote (Spotlight) vindo da nave
    vec3 vetorParaLuz = posicaoLuzNave - posicaoMundo;
    float distanciaLuz = length(vetorParaLuz);
    vec3 dirLuz = normalize(vetorParaLuz);

    float anguloCone = dot(normalize(-dirLuz), normalize(direcaoLuzNave));
    float intensidadeCone = smoothstep(0.72, 0.96, anguloCone);
    float atenuacao = 1.0 / (1.0 + 0.025 * distanciaLuz + 0.002 * distanciaLuz * distanciaLuz);

    float difusaHolofote = max(dot(norm, dirLuz), 0.0);
    vec3 luzHolofote = vec3(0.70, 0.95, 1.00) * difusaHolofote * intensidadeCone * atenuacao * 3.2;

    // Reflexo especular úmido/orgânico na pele quando atingida pelo farol da nave
    vec3 meiaVista = normalize(dirLuz + dirVisao);
    float especular = pow(max(dot(norm, meiaVista), 0.0), 28.0) * intensidadeCone * atenuacao;
    vec3 brilhoUmido = vec3(0.55, 0.95, 0.92) * especular * 0.9;

    // Efeito Fresnel (contorno ciano abissal nas bordas da silhueta)
    float fresnel = pow(1.0 - max(dot(norm, dirVisao), 0.0), 3.0);
    vec3 luzBorda = vec3(0.10, 0.55, 0.65) * fresnel * 0.55;

    // Pontos bioluminescentes ciano ao longo da lateral do corpo e pontas das nadadeiras
    float bioluminescencia = 0.0;
    if (posicaoLocal.z > -24.0 && posicaoLocal.z < 10.2 && abs(posicaoLocal.x) > 0.65) {
        // Fileira de pontos luminosos acompanhando a curvatura do pescoço e tronco
        float celulaZ = round(posicaoLocal.z / 2.1) * 2.1;
        float curvaY = (posicaoLocal.z > 2.0)
            ? 0.4 + (posicaoLocal.z - 2.0) * 0.18
            : 0.4 + sin(posicaoLocal.z * 0.25) * 0.35;

        float distPonto = length(vec2(posicaoLocal.y - curvaY, (posicaoLocal.z - celulaZ) * 0.85));
        float ponto = smoothstep(0.26, 0.06, distPonto);
        float halo = smoothstep(0.55, 0.10, distPonto) * 0.35;
        bioluminescencia += (ponto + halo);
    }

    // Brilho bioluminescente nas pontas das membranas/nadadeiras inferiores
    if (posicaoLocal.y < -2.2) {
        float padraoPontas = sin(posicaoLocal.z * 3.5) * cos(posicaoLocal.x * 2.5);
        if (padraoPontas > 0.65) {
            bioluminescencia += smoothstep(0.65, 0.95, padraoPontas) * 0.8;
        }
    }

    vec3 corBiolum = vec3(0.25, 0.92, 1.00) * clamp(bioluminescencia, 0.0, 1.2);

    // Combinação final (ambiente escuro do espaço + holofote da nave + bioluminescência)
    float luzAmbienteSuave = 0.12 + 0.10 * max(norm.y, 0.0);
    vec3 corFinal = corVertice * (vec3(luzAmbienteSuave) + luzHolofote) + brilhoUmido + luzBorda + corBiolum;

    corSaida = vec4(corFinal, 1.0);
}