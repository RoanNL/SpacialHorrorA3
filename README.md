# SpacialHorrorA3

**Alunos:**
* Erick Fonseca Dantas de Assis
* Pedro Vinícius Monte dos Reis
* Roan Nascimento Lisboa
* Hélder Arion de Brito Lima
* Rodson de Souza Silva Junior

Aplicação gráfica 3D em tempo real desenvolvida em Java com OpenGL 4.0 Core Profile (via JOGL e JOML) para a UC de Computação Gráfica e Realidade Virtual.

O projeto retrata um cenário espacial com temática de terror cósmico, combinando modelos 3D exportados do Blender (`.obj`/`.mtl`), geometria procedural e shaders em GLSL. Ao iniciar pelo menu principal, o usuário pode escolher entre duas cenas:

* **Cena Original (`Renderizador`):** Encontro entre a nave exploradora (com farol volumétrico) e a criatura abissal bioluminescente, com campo estelar e sistema planetário orbitando ao fundo.
* **Animação em Loop (`AnimacaoLoop`):** Sequência contínua de 67 segundos com salto no hiperespaço, sobrevoo pelos planetas, surgimento da criatura que devora o Sol e fuga pelo portal.

---

## Tecnologias Utilizadas
* Java 25 LTS (versão configurada em `<maven.compiler.release>` no `pom.xml`)
* Apache Maven (gerenciamento de dependências e build)
* JOGL 2.6.0 e GlueGen 2.6.0 (binding OpenGL 4.0)
* JOML 1.10.8 (álgebra vetorial e matrizes 3D)
* GLSL 4.0 (`#version 400 core`)
* Blender (modelagem das malhas `.obj` e `.mtl`)

---

## Estrutura de Pastas e Arquivos

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
* **`MenuPrincipal.java`:** Ponto de entrada unificado. Abre uma janela de seleção para escolher entre a Cena Original e a Animação em Loop. Ao pressionar `ESC` ou fechar a janela da cena, retorna ao menu automaticamente.
* **`Principal.java` e `PrincipalLoop.java`:** Classes de inicialização independentes que configuram o `GLJPanel` (1290x1080) e iniciam o `FPSAnimator` a 60 FPS.
* **`Renderizador.java`:** Implementa o `GLEventListener` da cena original, gerenciando VAOs/VBOs, câmera, iluminação, estrelas procedurais, sistema planetário e cone do holofote.
* **`AnimacaoLoop.java`:** Controla a linha do tempo de 67 segundos, câmera cinematográfica com 10 tomadas, abertura da mandíbula da criatura, sucção do Sol e efeitos de hiperespaço.
* **`Trajeto.java`:** Calcula posições e direções suaves para o voo da nave e da criatura usando splines cúbicas de Hermite com tangentes de Catmull-Rom.
* **`OBJLoader.java`:** Lê os modelos `.obj`/`.mtl`, converte faces de 4 ou mais vértices em triângulos, centraliza os astros na origem e monta o array intercalado de vértices (posição, cor e normal).
* **`GerenciadorShaders.java`:** Lê os arquivos `.glsl` da pasta `src/main/resources/shaders/`, compila na GPU e reporta erros no console.

---

## Pré-requisitos e Instalação

Antes de rodar o projeto, certifique-se de possuir uma placa de vídeo com suporte a **OpenGL 4.0 Core Profile** (com os drivers atualizados) e instale as ferramentas abaixo:

### 1. Java Development Kit (JDK 25 LTS)
O projeto está configurado para compilar com o **JDK 25** (conforme `<maven.compiler.release>` no `pom.xml`). Escolha uma das opções oficiais abaixo:

* **Eclipse Temurin / Adoptium (Recomendado):** [Baixar JDK 25 LTS no Adoptium](https://adoptium.net/pt-BR/temurin/releases)
  1. Selecione a versão **25 - LTS**, o seu sistema operacional (Windows/Linux/macOS) e o pacote **JDK**.
  2. No Windows, baixe o instalador **`.msi`**. Durante a instalação, marque as opções **"Add to PATH"** e **"Set JAVA_HOME variable"**.
* **Oracle JDK (Alternativa):** [Baixar na página oficial da Oracle](https://www.oracle.com/java/technologies/downloads/)

### 2. Apache Maven
Responsável por baixar automaticamente as bibliotecas do projeto (JOGL, GlueGen, JOML e Gson) e compilar o código.

* **Download oficial:** [Baixar Apache Maven](https://maven.apache.org/download.cgi)
* **Guia oficial de instalação:** [Documentação de Instalação do Maven](https://maven.apache.org/install.html)
  1. Na seção **Files**, baixe o arquivo **Binary zip archive** (`apache-maven-x.x.x-bin.zip`).
  2. Extraia o conteúdo para uma pasta fixa no seu computador (ex.: `C:\apache-maven`).
  3. Adicione a subpasta `bin` (ex.: `C:\apache-maven\bin`) à variável de ambiente **`PATH`** do sistema.

### 3. Git (para clonar o repositório)
* **Download oficial:** [Baixar Git](https://git-scm.com/install/)
  1. Selecione seu sistema operacional, baixe o instalador e conclua com as opções padrão.

### 4. Editor de Código (Opcional)
* **Visual Studio Code:** [Baixar VS Code](https://code.visualstudio.com/download) (recomendado instalar a extensão *Extension Pack for Java* caso queira editar ou executar pela IDE).

### Verificando a Instalação
Feche e abra um novo terminal (Prompt de Comando, PowerShell ou Bash) e execute:

```bash
java -version
mvn -version
git --version
```

Se todos os comandos exibirem suas respectivas versões (com o Java na versão **25** ou superior compatível), o ambiente está pronto.

---

## Como Rodar o Projeto

### 1. Clonar o repositório (ou baixar o ZIP)
```bash
git clone <URL_DO_REPOSITORIO>
cd SpacialHorrorA3
```

### 2. Verificação dos Arquivos
Antes de executar, certifique-se de que:
* Todos os arquivos `.obj` e `.mtl` estão em `src/main/resources/`.
* Os shaders (`vertex.glsl`, `fragment.glsl`, `loop_vertex.glsl`, `loop_fragment.glsl`) estão em `src/main/resources/shaders/`.

### 3. Comandos de Execução
No terminal, a partir da pasta raiz do projeto (`SpacialHorrorA3/`):

1. **Executar pelo Menu Principal (escolha de cena):**
```bash
mvn clean compile exec:java -Dexec.mainClass=com.SpacialHorrorA3.MenuPrincipal
```
*(Se `MenuPrincipal` já estiver definido como `<mainClass>` no `pom.xml`, basta rodar `mvn compile exec:java`.)*

2. **Executar diretamente a Cena Original:**
```bash
mvn compile exec:java -Dexec.mainClass=com.SpacialHorrorA3.Principal
```

3. **Executar diretamente a Animação em Loop:**
```bash
mvn compile exec:java -Dexec.mainClass=com.SpacialHorrorA3.PrincipalLoop
```

4. **Iniciar a Animação em Loop a partir de um segundo específico (ex.: 40s):**
```bash
mvn compile exec:java -Dexec.mainClass=com.SpacialHorrorA3.PrincipalLoop -Dinicio=40
```

5. **Gerar o `.jar` executável (na pasta `target/`):**
```bash
mvn clean package
```

### Controles
* **`ESC` ou fechar a janela (`X`):** encerra a cena atual e retorna ao menu principal (quando aberto pelo `MenuPrincipal`).