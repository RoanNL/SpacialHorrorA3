# SpacialHorrorA3

**Alunos:** 
* Erick Fonseca Dantas de Assis 
* Pedro Vinícius Monte dos Reis 
* Roan Nascimento Lisboa 
* Hélder Arion de Brito Lima 
* Rodson de Souza Silva Junior 

Aplicação gráfica 3D em tempo real desenvolvida em Java com OpenGL 4.0 Core Profile (via JOGL e JOML) para a disciplina de Computação Gráfica e Realidade Virtual (UNIFACS).

O projeto retrata um cenário espacial com temática de terror cósmico, combinando modelos 3D exportados do Blender (`.obj`/`.mtl`), geometria procedural e shaders em GLSL. Ao iniciar pelo menu principal, o usuário pode escolher entre duas cenas:

* Cena Original (`Renderizador`): Encontro entre a nave exploradora (com farol volumétrico) e a criatura abissal bioluminescente, com campo estelar e sistema planetário orbitando ao fundo.

* Animação em Loop (`AnimacaoLoop`): Sequência contínua de 67 segundos com salto no hiperespaço, sobrevoo pelos planetas, surgimento da criatura que devora o Sol e fuga pelo portal.
---
## Tecnologias Utilizadas
* Java (versão configurada no `pom.xml`)
* Apache Maven (gerenciamento de dependências e build)
* JOGL 2.6.0 e GlueGen 2.6.0 (binding OpenGL 4.0)
* JOML 1.10.8 (álgebra vetorial e matrizes 3D)
* GLSL 4.0 (`#version 400 core`)
* Blender (modelagem das malhas `.obj` e `.mtl`)
---
**Estrutura de Pastas e Arquivos**
```text
SpacialHorrorA3/
├── pom.xml                                    # Dependências Maven, plugins e classe principal
└── src/
    └── main/
        ├── java/com/SpacialHorrorA3/
        │   ├── MenuPrincipal.java             # Menu inicial em Swing para escolher a cena
        │   ├── Principal.java                 # Executa diretamente a Cena Original
        │   ├── PrincipalLoop.java             # Executa diretamente a Animação em Loop (MSAA 4x)
        │   ├── Renderizador.java              # Motor OpenGL da Cena Original
        │   ├── AnimacaoLoop.java              # Motor OpenGL da Animação em Loop (67s)
        │   ├── Trajeto.java                   # Curvas suaves (Hermite / Catmull-Rom) para o loop
        │   ├── OBJLoader.java                 # Leitura de .obj/.mtl, triangulação e buffer VBO
        │   └── GerenciadorShaders.java        # Compilação e linkagem dos shaders GLSL
        └── resources/
            ├── Nave_01.obj                    # Modelo 3D da nave exploradora
            ├── space-monster-final1.obj       # Modelo 3D da criatura espacial
            ├── Sol.obj                        # Modelo 3D do Sol
            ├── planeta1.obj                   # Planeta terrestre com árvores e lua
            ├── planet2.obj                    # Planeta vulcânico com crateras e lava
            ├── planeta3.obj                   # Planeta gasoso com anéis
            ├── *.mtl                          # Materiais correspondentes aos arquivos .obj
            └── shaders/
                ├── vertex.glsl                # Vertex Shader da Cena Original
                ├── fragment.glsl              # Fragment Shader da Cena Original
                ├── loop_vertex.glsl           # Vertex Shader da Animação em Loop
                └── loop_fragment.glsl         # Fragment Shader da Animação em Loop
```
## Descrição dos Arquivos Principais
`MenuPrincipal.java`: Ponto de entrada unificado. Abre uma janela de seleção para escolher entre a Cena Original e a Animação em Loop. Ao pressionar `ESC` ou fechar a janela da cena, retorna ao menu automaticamente.

`Principal.java` e `PrincipalLoop.java`: Classes de inicialização independentes que configuram o `GLJPanel` (1290x1080) e iniciam o `FPSAnimator` a 60 FPS.

`Renderizador.java`: Implementa o `GLEventListener` da cena original, gerenciando VAOs/VBOs, câmera, iluminação, estrelas procedurais, sistema planetário e cone do holofote.

`AnimacaoLoop.java`: Controla a linha do tempo de 67 segundos, câmera cinematográfica com 10 tomadas, abertura da mandíbula da criatura, sucção do Sol e efeitos de hiperespaço.

`Trajeto.java`: Calcula posições e direções suaves para o voo da nave e da criatura usando splines cúbicas de Hermite com tangentes de Catmull-Rom.

`OBJLoader.java`: Lê os modelos `.obj`/`.mtl`, converte faces de 4 ou mais vértices em triângulos, centraliza os astros na origem e monta o array intercalado de vértices (posição, cor e normal).

`GerenciadorShaders.java`: Lê os arquivos `.glsl` da pasta `src/main/resources/shaders/`, compila na GPU e reporta erros no console.

---
## Como Rodar o Projeto
*Pré-requisitos*
* JDK instalado (compatível com o `<maven.compiler.release>` definido no `pom.xml`)
* Apache Maven instalado e configurado no `PATH`
* Placa de vídeo com suporte a OpenGL 4.0 Core Profile

*Verificação dos Arquivos*
Antes de executar, certifique-se de que:

* Todos os arquivos `.obj` e `.mtl` estão em `src/main/resources/`.
* Os shaders (`vertex.glsl`, `fragment.glsl`, `loop_vertex.glsl`, `loop_fragment.glsl`) estão em `src/main/resources/shaders/`.

*Comandos de Execução*
No terminal, a partir da pasta raiz do projeto (`SpacialHorrorA3/`):

1. Executar pelo Menu Principal (escolha de cena):
```bash
mvn clean compile exec:java -Dexec.mainClass=com.SpacialHorrorA3.MenuPrincipal
```

(Se `MenuPrincipal` já estiver definido como `<mainClass>` no `pom.xml`, basta rodar `mvn compile exec:java`.)

2. Executar diretamente a Cena Original:
```bash
mvn compile exec:java -Dexec.mainClass=com.SpacialHorrorA3.Principal
```

3. Executar diretamente a Animação em Loop:
```bash
mvn compile exec:java -Dexec.mainClass=com.SpacialHorrorA3.PrincipalLoop
```

4. Iniciar a Animação em Loop a partir de um segundo específico (ex.: 40s):
```bash
mvn compile exec:java -Dexec.mainClass=com.SpacialHorrorA3.PrincipalLoop -Dinicio=40
```

5. Gerar o `.jar` executável (na pasta `target/`):
```bash
mvn clean package
```

*Controles*
`ESC` ou fechar a janela (`X`): encerra a cena atual e retorna ao menu principal (quando aberto pelo `MenuPrincipal`).
>>>>>>> origin/develop
