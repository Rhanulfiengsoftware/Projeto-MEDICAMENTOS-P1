package br.edu.unifan.farmacia;

/**
 * Classe auxiliar para execução fora do Maven (ex.: alguns IDEs/JARs), pois
 * uma classe que estende {@code Application} não pode ser a classe principal
 * quando o JavaFX está no classpath.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
