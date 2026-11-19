#version 400 core

layout (location = 0) in vec3 posicao;
layout (location = 1) in vec3 cor;
layout (location = 2) in vec3 normal;

uniform mat4 matrizMVP;
uniform mat4 matrizModelo;

out vec3 corVertice;
out vec3 normalVertice;
out vec3 posicaoLocal;
out vec3 posicaoMundo;

void main() {
    posicaoLocal = posicao;
    vec4 mundo = matrizModelo * vec4(posicao, 1.0);
    posicaoMundo = mundo.xyz;

    gl_Position = matrizMVP * vec4(posicao, 1.0);
    corVertice = cor;
    normalVertice = mat3(matrizModelo) * normal;
}