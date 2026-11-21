package com.SpacialHorrorA3;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OBJLoader {

    public static float[] carregarDadosModelo(String nomeArquivo, float[] corPadrao) {
        List<Float> verticesTemp = new ArrayList<>();
        List<Float> normaisTemp = new ArrayList<>();
        List<Float> dadosFinais = new ArrayList<>();

        Map<String, float[]> coresMateriais = new HashMap<>();
        // Materiais da Nave_01
        coresMateriais.put("Carcaca_Branca", new float[]{0.92f, 0.94f, 0.96f});
        coresMateriais.put("Metal_Cinza", new float[]{0.38f, 0.42f, 0.46f});
        coresMateriais.put("Detalhe_Azul", new float[]{0.12f, 0.48f, 0.92f});
        coresMateriais.put("Detalhe_Vermelho", new float[]{0.90f, 0.15f, 0.15f});
        coresMateriais.put("Vidro_Cockpit", new float[]{0.15f, 0.85f, 0.95f});
        coresMateriais.put("Propulsor_Emissivo", new float[]{1.00f, 0.60f, 0.10f});

        // Materiais específicos do Monstro Espacial
        coresMateriais.put("Gums", new float[]{0.22f, 0.06f, 0.08f});         // Gengiva escura avermelhada
        coresMateriais.put("Pupil", new float[]{0.45f, 0.96f, 1.00f});        // Olho bioluminescente ciano claro
        coresMateriais.put("Material.001", new float[]{0.10f, 0.45f, 0.55f}); // Borda da íris

        // Materiais do Sol e dos Planetas (Sol.obj, planeta1.obj, planet2.obj, planeta3.obj)
        coresMateriais.put("sun", new float[]{1.00f, 0.78f, 0.15f});          // Sol emissivo
        coresMateriais.put("Land", new float[]{0.20f, 0.55f, 0.25f});         // Planeta 1: Continentes
        coresMateriais.put("Ocean", new float[]{0.10f, 0.35f, 0.75f});        // Planeta 1: Oceanos
        coresMateriais.put("Tree", new float[]{0.12f, 0.38f, 0.15f});         // Planeta 1: Árvores
        coresMateriais.put("Moon", new float[]{0.72f, 0.74f, 0.78f});         // Planeta 1: Lua
        coresMateriais.put("planet", new float[]{0.38f, 0.16f, 0.12f});       // Planeta 2: Rocha vulcânica
        coresMateriais.put("cratera", new float[]{0.20f, 0.08f, 0.06f});      // Planeta 2: Crateras
        coresMateriais.put("lava", new float[]{1.00f, 0.32f, 0.02f});         // Planeta 2: Lava emissiva
        coresMateriais.put("Material.005", new float[]{0.82f, 0.68f, 0.45f}); // Planeta 3: Corpo gasoso
        coresMateriais.put("Material.004", new float[]{0.65f, 0.55f, 0.42f}); // Planeta 3: Anéis

        float[] corAtual = corPadrao.clone();
        String materialAtual = "";
        String objetoAtual = "";
        boolean ehMonstro = nomeArquivo.contains("monster");
        boolean ehAstro = nomeArquivo.equals("Sol.obj") || nomeArquivo.startsWith("planet");

        try {
            InputStream is = OBJLoader.class.getResourceAsStream("/" + nomeArquivo);
            if (is == null) {
                System.err.println("ERRO: O arquivo " + nomeArquivo + " não foi encontrado em resources!");
                return new float[0];
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String linha;

            while ((linha = reader.readLine()) != null) {
                linha = linha.trim();
                if (linha.isEmpty() || linha.startsWith("#")) continue;

                String[] tokens = linha.split("\\s+");

                switch (tokens[0]) {
                    case "mtllib":
                        if (tokens.length > 1) {
                            carregarMTL(tokens[1], coresMateriais);
                        }
                        break;

                    case "o":
                        if (tokens.length > 1) {
                            objetoAtual = tokens[1];
                            if (objetoAtual.startsWith("Tooth")) {
                                corAtual = new float[]{0.80f, 0.90f, 0.92f};
                                materialAtual = "Tooth";
                            } else if (objetoAtual.contains("Gum")) {
                                corAtual = new float[]{0.22f, 0.06f, 0.08f};
                                materialAtual = "Gums";
                            }
                        }
                        break;

                    case "v":
                        verticesTemp.add(Float.parseFloat(tokens[1]));
                        verticesTemp.add(Float.parseFloat(tokens[2]));
                        verticesTemp.add(Float.parseFloat(tokens[3]));
                        break;

                    case "vn":
                        normaisTemp.add(Float.parseFloat(tokens[1]));
                        normaisTemp.add(Float.parseFloat(tokens[2]));
                        normaisTemp.add(Float.parseFloat(tokens[3]));
                        break;

                    case "usemtl":
                        if (tokens.length > 1) {
                            materialAtual = tokens[1];
                            corAtual = coresMateriais.getOrDefault(materialAtual, corPadrao.clone());
                        } else if (objetoAtual.contains("Gum")) {
                            materialAtual = "Gums";
                            corAtual = coresMateriais.get("Gums");
                        }
                        break;

                    case "f":
                        int numVerticesFace = tokens.length - 1;
                        for (int i = 2; i < numVerticesFace; i++) {
                            adicionarVertice(tokens[1], verticesTemp, normaisTemp, corAtual, materialAtual, objetoAtual, ehMonstro, dadosFinais);
                            adicionarVertice(tokens[i], verticesTemp, normaisTemp, corAtual, materialAtual, objetoAtual, ehMonstro, dadosFinais);
                            adicionarVertice(tokens[i + 1], verticesTemp, normaisTemp, corAtual, materialAtual, objetoAtual, ehMonstro, dadosFinais);
                        }
                        break;
                }
            }
            System.out.println("Modelo carregado: " + nomeArquivo + " | Triângulos: " + (dadosFinais.size() / 27));
        } catch (Exception e) {
            System.err.println("Erro ao carregar o modelo " + nomeArquivo + ": " + e.getMessage());
            e.printStackTrace();
        }

        float[] arrayFinal = new float[dadosFinais.size()];
        for (int i = 0; i < dadosFinais.size(); i++) {
            arrayFinal[i] = dadosFinais.get(i);
        }

        // Centraliza o Sol e os Planetas na origem (0,0,0) preservando a posição relativa da Lua e Anéis
        if (ehAstro && arrayFinal.length > 0) {
            centralizarAstro(arrayFinal);
        }

        return arrayFinal;
    }

    private static void centralizarAstro(float[] dados) {
        int totalVertices = dados.length / 9;
        // Usa os primeiros vértices (esfera principal do astro) para encontrar o centro real
        int limite = Math.min(totalVertices, 480);
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (int i = 0; i < limite; i++) {
            float x = dados[i * 9];
            float y = dados[i * 9 + 1];
            float z = dados[i * 9 + 2];
            if (x < minX) minX = x; if (x > maxX) maxX = x;
            if (y < minY) minY = y; if (y > maxY) maxY = y;
            if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;
        }

        float cx = (minX + maxX) / 2.0f;
        float cy = (minY + maxY) / 2.0f;
        float cz = (minZ + maxZ) / 2.0f;

        for (int i = 0; i < totalVertices; i++) {
            dados[i * 9]     -= cx;
            dados[i * 9 + 1] -= cy;
            dados[i * 9 + 2] -= cz;
        }
    }

    private static void adicionarVertice(String tokenVertice, List<Float> verticesTemp, List<Float> normaisTemp,
                                        float[] corBase, String materialAtual, String objetoAtual,
                                        boolean ehMonstro, List<Float> dadosFinais) {
        String[] partes = tokenVertice.split("/");
        int idxVertice = Integer.parseInt(partes[0]) - 1;

        float vx = verticesTemp.get(idxVertice * 3);
        float vy = verticesTemp.get(idxVertice * 3 + 1);
        float vz = verticesTemp.get(idxVertice * 3 + 2);

        // Posição (X, Y, Z) -> location = 0
        dadosFinais.add(vx);
        dadosFinais.add(vy);
        dadosFinais.add(vz);

        // Cor (R, G, B) -> location = 1
        float r = corBase[0];
        float g = corBase[1];
        float b = corBase[2];

        if (ehMonstro && "Space_Monster".equals(objetoAtual)) {
            r = 0.05f;
            g = 0.11f;
            b = 0.15f;

            if (vy > 1.2f) {
                float fatorCrista = Math.min(1.0f, (vy - 1.2f) / 3.0f);
                r += 0.42f * fatorCrista;
                g += 0.10f * fatorCrista;
                b += 0.06f * fatorCrista;
            }

            if (vz > -8.0f && vy < 1.8f && vy > -2.2f) {
                float fatorPeito = Math.max(0.0f, 1.0f - Math.abs(vz - 2.0f) / 10.0f);
                r += 0.08f * fatorPeito;
                g += 0.38f * fatorPeito;
                b += 0.42f * fatorPeito;
            }

            if (vy <= -1.8f) {
                r = 0.04f;
                g = 0.14f;
                b = 0.18f;
            }
        } else if (ehMonstro && "Eye".equals(objetoAtual) && "Material".equals(materialAtual)) {
            r = 0.03f;
            g = 0.06f;
            b = 0.09f;
        }

        dadosFinais.add(r);
        dadosFinais.add(g);
        dadosFinais.add(b);

        // Normal (NX, NY, NZ) -> location = 2
        // Propulsor, Pupila, Sol e Lava recebem normal (0,0,0) para ativar o brilho emissivo puro no fragment.glsl
        if ("Propulsor_Emissivo".equals(materialAtual) || "Pupil".equals(materialAtual)
                || "sun".equals(materialAtual) || "lava".equals(materialAtual)) {
            dadosFinais.add(0.0f);
            dadosFinais.add(0.0f);
            dadosFinais.add(0.0f);
        } else if (partes.length >= 3 && !partes[2].isEmpty()) {
            int idxNormal = Integer.parseInt(partes[2]) - 1;
            dadosFinais.add(normaisTemp.get(idxNormal * 3));
            dadosFinais.add(normaisTemp.get(idxNormal * 3 + 1));
            dadosFinais.add(normaisTemp.get(idxNormal * 3 + 2));
        } else {
            dadosFinais.add(0.0f);
            dadosFinais.add(1.0f);
            dadosFinais.add(0.0f);
        }
    }

    private static void carregarMTL(String nomeMtl, Map<String, float[]> mapaCores) {
        try {
            InputStream is = OBJLoader.class.getResourceAsStream("/" + nomeMtl);
            if (is == null) return;

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String linha;
            String matAtual = null;

            while ((linha = reader.readLine()) != null) {
                linha = linha.trim();
                if (linha.isEmpty() || linha.startsWith("#")) continue;
                String[] tokens = linha.split("\\s+");

                if (tokens[0].equals("newmtl") && tokens.length > 1) {
                    matAtual = tokens[1];
                } else if (tokens[0].equals("Kd") && matAtual != null) {
                    mapaCores.putIfAbsent(matAtual, new float[]{
                        Float.parseFloat(tokens[1]),
                        Float.parseFloat(tokens[2]),
                        Float.parseFloat(tokens[3])
                    });
                }
            }
        } catch (Exception ignored) {
        }
    }
}