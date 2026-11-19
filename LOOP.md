# Animação em loop

Animação nova feita **só com arquivos novos**: nenhuma classe, shader ou configuração original foi alterada.
Ela usa os mesmos modelos (nave, planetas, Sol, criatura) e o `OBJLoader` e o `GerenciadorShaders` originais.

## Arquivos novos

| Arquivo | O que faz |
|---|---|
| `src/main/java/com/SpacialHorrorA3/PrincipalLoop.java` | Abre a janela da animação em loop |
| `src/main/java/com/SpacialHorrorA3/AnimacaoLoop.java` | A animação inteira (roteiro, câmera, efeitos) |
| `src/main/java/com/SpacialHorrorA3/Trajeto.java` | Caminhos suaves por pontos-chave (nave, criatura) |
| `src/main/resources/shaders/loop_vertex.glsl` | Shader de vértices da animação |
| `src/main/resources/shaders/loop_fragment.glsl` | Shader de cor: Sol, criatura, buraco e túnel do hiperespaço |

## Executar

```
mvn compile exec:java -Dexec.mainClass=com.SpacialHorrorA3.PrincipalLoop
```

O `mvn compile exec:java` sem esse parâmetro continua abrindo a animação original.
Para começar do meio enquanto ajusta uma cena: acrescente `-Dinicio=40` (segundos).

## Roteiro (um ciclo de 67 s)

| Tempo (s) | Cena |
|---|---|
| 0 – 1,6   | Dentro do túnel do hiperespaço |
| 1,6 – 4   | Um buraco se abre no espaço e a nave sai dele; o buraco se fecha |
| 4 – 10    | A nave voa em direção ao sistema e passa sobre os anéis do planeta gasoso |
| 10 – 20   | Rasante pelo planeta vulcânico, perto do Sol |
| 20 – 29   | Passa por fora do planeta com lua (câmera parada à beira do caminho) |
| 29 – 37   | Curva, freia e para de frente para o Sol |
| 37 – 47   | Contemplação; a criatura se forma no meio das estrelas |
| 46 – 58   | A nave liga o farol; a criatura chega, abre a boca e devora o Sol |
| 58 – 61   | Escuridão: a criatura encara a nave; o farol falha |
| 61 – 66   | A nave foge, a criatura dá o bote de boca aberta, outro buraco se abre |
| 66 – 67   | A nave mergulha no buraco e a câmera entra no túnel do hiperespaço |

## Escala

- **Nave:** escala 0,06, com envergadura de cerca de 0,85.
- **Planetas:** raios entre 6 e 8,5, ou seja, de 7 a 10 vezes a envergadura da nave.
- **Sol:** raio 16, maior que todos os planetas.
- **Criatura:** cerca de 120 de comprimento, bem maior que o próprio Sol.

Para mudar, ajuste as constantes `ESCALA_*` e os planetas no topo de `AnimacaoLoop.java`.

## Por que o loop não tem emenda

O ciclo começa e termina **dentro do túnel do hiperespaço**, que cobre a tela inteira.
O túnel só usa movimentos que completam um número inteiro de voltas por ciclo, por isso o último quadro é idêntico ao primeiro.
Para tocar uma vez só e fechar a janela no fim, mude `REPETIR_EM_LOOP` para `false` em `AnimacaoLoop.java`.
