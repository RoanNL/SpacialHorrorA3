#version 400 core

in vec3 corVertice;
in vec3 normalVertice;
in vec3 posicaoLocal;
in vec3 posicaoMundo;

// Modos de desenho:
//  0 = estrelas         1 = nave e planetas (luz do Sol)
//  2 = criatura         3 = feixe do farol (translúcido)
//  4 = brilho/halo      5 = superfície do Sol
//  6 = buraco de hiperespaço (disco virado para a câmera)
//  7 = túnel do hiperespaço cobrindo a tela
uniform int usarIluminacao;
uniform vec3 posicaoCamera;
uniform float tempo;             // segundos dentro do ciclo do loop (0..periodo)
uniform float periodo;           // duração do ciclo
uniform float fade;              // 0 = tela preta, 1 = normal
uniform float aspecto;           // largura / altura da janela (para o túnel não ficar achatado)

// Farol da nave
uniform vec3 posicaoLuzNave;
uniform vec3 direcaoLuzNave;
uniform float intensidadeFarol;  // 0 = desligado
uniform float alcanceFeixe;

// Sol (é a luz principal da cena; apaga quando é devorado)
uniform vec3 posicaoSol;
uniform vec3 corSol;
uniform float intensidadeSol;

// Criatura: brilho da energia do Sol absorvida
uniform float energiaAbsorvida;
// Criatura se materializando a partir das estrelas: 0 = invisível, 1 = inteira
uniform float surgimento;

// Uso geral: luz de preenchimento (modo 1) ou força do brilho (modos 4 e 5)
uniform float intensidade;

out vec4 corSaida;

// Ângulo que dá exatamente 'voltas' voltas por ciclo: animações periódicas usam isso
// para o último quadro do loop emendar perfeitamente no primeiro.
float fase(float voltas) {
    return 6.2831853 * voltas * tempo / periodo;
}

// Ruído 3D suave (valor entre 0 e 1) usado para a criatura se formar aos poucos
float hash3(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}
float ruido3(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash3(i + vec3(0, 0, 0)), hash3(i + vec3(1, 0, 0)), f.x),
                   mix(hash3(i + vec3(0, 1, 0)), hash3(i + vec3(1, 1, 0)), f.x), f.y),
               mix(mix(hash3(i + vec3(0, 0, 1)), hash3(i + vec3(1, 0, 1)), f.x),
                   mix(hash3(i + vec3(0, 1, 1)), hash3(i + vec3(1, 1, 1)), f.x), f.y), f.z);
}

void main() {
    // ---------------- Modo 7: túnel do hiperespaço (tela cheia) ----------------
    // Riscos de luz correndo para fora a partir do centro. Todas as velocidades são inteiras
    // por ciclo, então o túnel no fim do loop é idêntico ao do começo.
    if (usarIluminacao == 7) {
        vec2 p = posicaoLocal.xy * vec2(aspecto, 1.0);
        float r = length(p);
        float ang = atan(p.y, p.x) / 6.2831853 + 0.5;
        float fluxo = tempo / periodo * 40.0;          // 40 "passagens" por ciclo
        float profundidade = 0.45 / max(r, 0.002);     // centro = fundo do túnel

        float faixas = 140.0;
        float faixa = floor(ang * faixas);
        float h = fract(sin(faixa * 12.9898) * 43758.5453);
        float velocidade = 1.0 + floor(h * 3.0);       // 1, 2 ou 3 (inteiro)
        float mov = fract(profundidade * 0.5 + h * 7.0 - fluxo * velocidade);
        float risco = smoothstep(0.0, 0.04, mov) * (1.0 - smoothstep(0.04, 0.45, mov));
        float fino = 1.0 - smoothstep(0.0, 0.18, abs(fract(ang * faixas) - 0.5));
        risco *= fino * step(0.35, h) * smoothstep(0.03, 0.4, r);

        float paredes = 0.5 + 0.5 * sin(profundidade * 9.0 - 6.2831853 * fluxo);
        vec3 fundo = mix(vec3(0.01, 0.02, 0.07), vec3(0.06, 0.10, 0.30), paredes * smoothstep(0.1, 1.2, r));
        vec3 c = fundo
               + vec3(0.55, 0.75, 1.00) * risco * 1.6
               + vec3(0.80, 0.90, 1.00) * exp(-r * 5.0) * 0.9;   // luz no fim do túnel
        corSaida = vec4(c, intensidade);
        return;
    }

    // ---------------- Modo 6: buraco de hiperespaço ----------------
    // Disco escuro com um redemoinho azul/violeta girando e uma borda branca brilhante.
    if (usarIluminacao == 6) {
        vec2 p = posicaoLocal.xy;
        float r = length(p);
        if (r >= 1.0) discard;
        float ang = atan(p.y, p.x);
        float espiral = 0.5 + 0.5 * sin(ang * 4.0 + r * 16.0 - fase(30.0));
        float borda = exp(-pow((r - 0.86) / 0.07, 2.0));
        float miolo = 1.0 - smoothstep(0.55, 0.9, r);
        vec3 redemoinho = mix(vec3(0.20, 0.10, 0.45), vec3(0.20, 0.45, 1.00), espiral);
        vec3 c = vec3(0.01, 0.02, 0.06) * miolo
               + redemoinho * miolo * (0.25 + 0.55 * r) * espiral
               + vec3(0.85, 0.93, 1.00) * borda * 1.6 * intensidade
               + vec3(0.70, 0.85, 1.00) * exp(-r * 8.0) * 0.8 * (intensidade - 1.0);  // clarão
        float a = clamp(miolo * 0.94 + borda, 0.0, 1.0);
        corSaida = vec4(c * fade, a * clamp(intensidade, 0.0, 1.0));
        return;
    }

    // ---------------- Modo 0: estrelas ----------------
    if (usarIluminacao == 0) {
        corSaida = vec4(corVertice * fade, 1.0);
        return;
    }

    // ---------------- Modo 3: feixe volumétrico do farol ----------------
    if (usarIluminacao == 3) {
        float dist = length(posicaoMundo - posicaoLuzNave);
        float fadeDist = clamp(1.0 - dist / alcanceFeixe, 0.0, 1.0);
        corSaida = vec4(corVertice * fade, fadeDist * fadeDist * 0.11 * intensidadeFarol);
        return;
    }

    // ---------------- Modo 4: halo (coroa do Sol, brilho na boca) ----------------
    // Desenhado num quadrado virado para a câmera, coordenadas locais de -1 a 1.
    if (usarIluminacao == 4) {
        float r = length(posicaoLocal.xy);
        if (r >= 1.0) discard;
        float ang = atan(posicaoLocal.y, posicaoLocal.x);
        float raios = 0.78 + 0.22 * sin(ang * 9.0 + fase(2.0)) * sin(ang * 5.0 - fase(1.0));
        float halo = pow(1.0 - r, 2.6) * raios;
        float nucleo = exp(-r * 9.0);
        float a = (halo * 0.75 + nucleo * 0.5) * intensidade;
        corSaida = vec4(corSol * fade, clamp(a, 0.0, 1.0));
        return;
    }

    vec3 dirVisao = normalize(posicaoCamera - posicaoMundo);

    // ---------------- Modo 5: superfície do Sol ----------------
    if (usarIluminacao == 5) {
        vec3 n = normalize(normalVertice);
        float mu = max(dot(n, dirVisao), 0.0);
        // Escurecimento de borda: núcleo quase branco, meio amarelo, borda laranja
        vec3 centro = vec3(1.06, 0.97, 0.74);
        vec3 meio = vec3(1.00, 0.72, 0.22);
        vec3 borda = vec3(0.95, 0.36, 0.05);
        vec3 c = mix(borda, meio, smoothstep(0.0, 0.45, mu));
        c = mix(c, centro, smoothstep(0.45, 1.0, mu));
        // Granulação animada: manchas mais quentes/mais frias (muda o tom, não acinzenta)
        vec3 p = posicaoLocal * 0.55;
        float g = sin(p.x * 1.7 + fase(12.0)) * sin(p.y * 1.9 - fase(9.0)) * sin(p.z * 1.5 + fase(7.0));
        g += 0.5 * sin(p.x * 4.1 - fase(17.0)) * sin(p.z * 3.7 + fase(14.0));
        c = mix(c, borda, clamp(-g, 0.0, 1.0) * 0.18);
        c *= 1.0 + 0.06 * g;
        // Tinge com a cor atual do Sol (avermelha quando está sendo devorado)
        c *= corSol / vec3(1.0, 0.9, 0.72);
        corSaida = vec4(c * intensidade * fade, 1.0);
        return;
    }

    // ---------------- Criatura se materializando ----------------
    // Cada ponto do corpo "acende" quando 'surgimento' passa do seu valor de ruído.
    // A cauda se forma primeiro e a cabeça (com os olhos) por último.
    // Na borda que avança, a pele brilha como poeira de estrelas.
    float brilhoFormacao = 0.0;
    if (usarIluminacao == 2 && surgimento < 1.0) {
        float n = ruido3(posicaoLocal * 0.9) * 0.55 + ruido3(posicaoLocal * 2.7) * 0.2
                + 0.25 * smoothstep(-27.0, 12.0, posicaoLocal.z);
        float limiar = mix(0.22, 1.05, surgimento);
        if (n > limiar) discard;
        float cintilar = 0.6 + 0.4 * sin(fase(117.0) + n * 60.0);
        brilhoFormacao = smoothstep(limiar - 0.05, limiar, n) * cintilar;
    }

    // Normal zero = material emissivo puro (propulsor, lava, pupila da criatura)
    if (length(normalVertice) < 0.1) {
        vec3 c = corVertice;
        if (usarIluminacao == 2) {
            // O olho absorve o brilho do Sol devorado
            c = mix(c, vec3(1.00, 0.62, 0.22), clamp(energiaAbsorvida, 0.0, 1.0) * 0.85) * (1.0 + energiaAbsorvida * 0.8);
            c += vec3(0.75, 0.90, 1.00) * brilhoFormacao * 1.6;
        }
        corSaida = vec4(c * fade, 1.0);
        return;
    }

    vec3 norm = normalize(normalVertice);
    vec3 dirSol = normalize(posicaoSol - posicaoMundo);

    // ---------------- Modo 1: nave e planetas ----------------
    if (usarIluminacao == 1) {
        if (dot(norm, dirVisao) < 0.0) norm = -norm;  // ilumina os dois lados (ex.: anéis)
        float difusa = max(dot(norm, dirSol), 0.0);
        vec3 luz = vec3(0.045, 0.055, 0.085)                // breu do espaço
                 + vec3(0.55, 0.65, 0.85) * intensidade     // luz de preenchimento
                 + corSol * intensidadeSol * difusa * 1.15; // Sol
        float fresnel = pow(1.0 - max(dot(norm, dirVisao), 0.0), 3.0);
        vec3 corFinal = corVertice * luz + vec3(0.05, 0.09, 0.14) * fresnel * 0.6;
        corSaida = vec4(corFinal * fade, 1.0);
        return;
    }

    // ---------------- Modo 2: criatura abissal ----------------
    // 1. Holofote (spotlight) vindo da nave
    vec3 vetorParaLuz = posicaoLuzNave - posicaoMundo;
    float distanciaLuz = length(vetorParaLuz);
    vec3 dirLuz = normalize(vetorParaLuz);

    float anguloCone = dot(normalize(-dirLuz), normalize(direcaoLuzNave));
    float intensidadeCone = smoothstep(0.72, 0.96, anguloCone);
    float atenuacao = 1.0 / (1.0 + 0.025 * distanciaLuz + 0.0012 * distanciaLuz * distanciaLuz);

    float difusaHolofote = max(dot(norm, dirLuz), 0.0);
    vec3 luzHolofote = vec3(0.70, 0.95, 1.00) * difusaHolofote * intensidadeCone * atenuacao * 3.2 * intensidadeFarol;

    // Reflexo úmido na pele quando atingida pelo farol
    vec3 meiaVista = normalize(dirLuz + dirVisao);
    float especular = pow(max(dot(norm, meiaVista), 0.0), 28.0) * intensidadeCone * atenuacao * intensidadeFarol;
    vec3 brilhoUmido = vec3(0.55, 0.95, 0.92) * especular * 0.9;

    // 2. Fresnel: contorno ciano abissal + contraluz quando o Sol está atrás da criatura
    float fresnel = pow(1.0 - max(dot(norm, dirVisao), 0.0), 3.0);
    float contraLuz = pow(max(dot(-dirVisao, dirSol), 0.0), 2.0);
    vec3 luzBorda = vec3(0.10, 0.55, 0.65) * fresnel * 0.40
                  + corSol * intensidadeSol * fresnel * (0.15 + 1.4 * contraLuz);

    // Luz direta do Sol na pele
    float difusaSol = max(dot(norm, dirSol), 0.0);
    vec3 luzSol = corSol * intensidadeSol * difusaSol * 0.30;

    // 3. Pontos bioluminescentes ao longo do corpo e nas pontas das nadadeiras
    float bioluminescencia = 0.0;
    if (posicaoLocal.z > -24.0 && posicaoLocal.z < 10.2 && abs(posicaoLocal.x) > 0.65) {
        float celulaZ = round(posicaoLocal.z / 2.1) * 2.1;
        float curvaY = (posicaoLocal.z > 2.0)
            ? 0.4 + (posicaoLocal.z - 2.0) * 0.18
            : 0.4 + sin(posicaoLocal.z * 0.25) * 0.35;

        float distPonto = length(vec2(posicaoLocal.y - curvaY, (posicaoLocal.z - celulaZ) * 0.85));
        float ponto = smoothstep(0.26, 0.06, distPonto);
        float halo = smoothstep(0.55, 0.10, distPonto) * 0.35;
        bioluminescencia += (ponto + halo);
    }
    if (posicaoLocal.y < -2.2) {
        float padraoPontas = sin(posicaoLocal.z * 3.5) * cos(posicaoLocal.x * 2.5);
        if (padraoPontas > 0.65) {
            bioluminescencia += smoothstep(0.65, 0.95, padraoPontas) * 0.8;
        }
    }
    // Pulsação lenta; depois de devorar o Sol os pontos ficam dourados e muito mais fortes
    float pulso = 0.85 + 0.15 * sin(fase(29.0) - posicaoLocal.z * 0.35);
    vec3 corBio = mix(vec3(0.25, 0.92, 1.00), vec3(1.00, 0.70, 0.25), clamp(energiaAbsorvida, 0.0, 1.0));
    vec3 corBiolum = corBio * clamp(bioluminescencia, 0.0, 1.2) * pulso * (1.0 + energiaAbsorvida * 2.0);

    // 4. Combinação final
    float luzAmbienteSuave = 0.10 + 0.08 * max(norm.y, 0.0);
    vec3 corFinal = corVertice * (vec3(luzAmbienteSuave) + luzHolofote + luzSol) + brilhoUmido + luzBorda + corBiolum;
    corFinal += vec3(0.75, 0.90, 1.00) * brilhoFormacao * 1.6;  // poeira de estrelas na borda

    corSaida = vec4(corFinal * fade, 1.0);
}
