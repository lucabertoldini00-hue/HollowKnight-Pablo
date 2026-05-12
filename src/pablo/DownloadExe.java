// DownloadExe.java
// Compila il progetto HollowKnight-Pablo, crea un fat JAR e lo impacchetta
// come Pablo.exe sul Desktop tramite Launch4j.
//
// Esecuzione: avviare dalla root del progetto (quella che contiene src/ e assets/).

package pablo;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DownloadExe {

    private static final String MAIN_CLASS  = "pablo.Launcher";
    private static final String EXE_NAME    = "Pablo.exe";
    private static final String ICON        = "assets/mask.png";
    private static final String LAUNCH4J_URL =
            "https://downloads.sourceforge.net/project/launch4j/launch4j-3/3.14/launch4j-3.14-win32.zip";

    // =========================================================================
    // Entry point
    // =========================================================================

    public static void main(String[] args) throws Exception {
        Path project = findProjectRoot();
        Path src     = project.resolve("src");
        Path dest    = desktopDirectory();
        Path tmp     = Files.createTempDirectory("PabloExeBuilder-");

        try {
            Path classes = tmp.resolve("classes");
            Path jar     = tmp.resolve("Pablo.jar");
            Path ico     = tmp.resolve("mask.ico");

            System.out.println("Progetto: " + project);

            require(Files.isRegularFile(src.resolve("pablo").resolve("Launcher.java")),
                    "Launcher.java non trovato in " + src);
            require(Files.isRegularFile(project.resolve(ICON)),
                    "Icona non trovata: " + ICON);

            List<Path> jars = findDependencyJars(project);
            require(!jars.isEmpty(),
                    "Nessuna libreria trovata. Controlla .idea/libraries/ o la cartella GdxLibs.");

            Files.deleteIfExists(dest.resolve(EXE_NAME));
            Files.createDirectories(classes);

            compileProject(src, classes, jars);
            createFatJar(classes, jars, project.resolve("assets"), jar);
            createIco(project.resolve(ICON), ico);

            Path launch4j = ensureLaunch4j(tmp);
            Path config   = tmp.resolve("launch4j.xml");
            writeLaunch4jConfig(config, jar, dest.resolve(EXE_NAME), ico);
            run(Arrays.asList(launch4j.toString(), config.toString()), project);

            require(Files.isRegularFile(dest.resolve(EXE_NAME)),
                    "Pablo.exe non è stato creato.");
            require(hasMzHeader(dest.resolve(EXE_NAME)),
                    "Pablo.exe non è un eseguibile Windows valido.");

            System.out.println("Creato: " + dest.resolve(EXE_NAME));
        } finally {
            delete(tmp);
        }
    }

    // =========================================================================
    // Percorsi
    // =========================================================================

    /** Restituisce il Desktop dell'utente corrente (supporta anche OneDrive). */
    private static Path desktopDirectory() {
        Path desktop = Paths.get(System.getProperty("user.home"), "Desktop");
        if (Files.isDirectory(desktop)) return desktop.toAbsolutePath().normalize();

        Path oneDrive = Paths.get(System.getProperty("user.home"), "OneDrive", "Desktop");
        if (Files.isDirectory(oneDrive)) return oneDrive.toAbsolutePath().normalize();

        throw new IllegalStateException("Desktop non trovato per l'utente corrente.");
    }

    /** Risale le directory fino a trovare la root del progetto. */
    private static Path findProjectRoot() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve("src"))
                    && Files.isDirectory(current.resolve("assets"))
                    && Files.isRegularFile(current.resolve("src")
                    .resolve("pablo").resolve("Launcher.java"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException(
                "Eseguire DownloadExe dalla root del progetto HollowKnight-Pablo.");
    }

    // =========================================================================
    // Ricerca dipendenze
    // =========================================================================

    /** Raccoglie tutti i JAR di runtime dalle directory note del progetto. */
    private static List<Path> findDependencyJars(Path project) throws Exception {
        LinkedHashSet<Path> directories = new LinkedHashSet<>();

        // Directory dichiarate nei file .xml di IntelliJ
        Path ideaLibs = project.resolve(".idea").resolve("libraries");
        if (Files.isDirectory(ideaLibs)) {
            try (Stream<Path> files = Files.list(ideaLibs)) {
                files.filter(p -> p.getFileName().toString().endsWith(".xml"))
                        .forEach(p -> readLibraryXml(project, p, directories));
            }
        }

        // Directory convenzionali
        directories.add(project.resolve("GdxLibs"));
        directories.add(project.resolve("lib"));
        directories.add(project.resolve("libs"));

        // Cerca GdxLibs risalendo fino alla root del filesystem
        Path current = project;
        while (current != null) {
            directories.add(current.resolve("GdxLibs"));
            current = current.getParent();
        }

        // Cartella GdxLibs in Documenti utente
        String home = System.getProperty("user.home");
        if (home != null)
            directories.add(Paths.get(home).resolve("Documents").resolve("GdxLibs"));

        // Raccoglie i JAR validi da tutte le directory trovate
        LinkedHashSet<Path> result = new LinkedHashSet<>();
        for (Path dir : directories) {
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(Files::isRegularFile)
                        .filter(DownloadExe::isRuntimeJar)
                        .forEach(p -> result.add(p.toAbsolutePath().normalize()));
            }
        }

        List<Path> sorted = new ArrayList<>(result);
        Collections.sort(sorted);
        return sorted;
    }

    /** Legge un file .xml di IntelliJ e aggiunge le directory dei JAR all'insieme. */
    private static void readLibraryXml(Path project, Path xml, Set<Path> directories) {
        try {
            String text    = new String(Files.readAllBytes(xml), StandardCharsets.UTF_8);
            Matcher matcher = Pattern.compile("url=\"file://([^\"]+)\"").matcher(text);
            while (matcher.find()) {
                String raw = URLDecoder.decode(matcher.group(1), "UTF-8")
                        .replace("$PROJECT_DIR$", project.toString().replace('\\', '/'));
                // Su Windows i path iniziano con /C:/ → rimuovi lo slash iniziale
                if (raw.startsWith("/") && raw.length() > 2 && raw.charAt(2) == ':')
                    raw = raw.substring(1);
                if (!raw.toLowerCase(Locale.ROOT).endsWith(".jar"))
                    directories.add(Paths.get(raw).toAbsolutePath().normalize());
            }
        } catch (Exception ignored) { }
    }

    /** True se il JAR è di runtime (esclude sources e javadoc). */
    private static boolean isRuntimeJar(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".jar")
                && !name.contains("sources")
                && !name.contains("source")
                && !name.contains("javadoc");
    }

    // =========================================================================
    // Compilazione
    // =========================================================================

    /** Compila tutti i sorgenti .java di src/ nella directory classes/. */
    private static void compileProject(Path src, Path classes, List<Path> jars) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(findTool("javac.exe").toString());
        command.add("-encoding"); command.add("UTF-8");
        command.add("-source"); command.add("1.8");
        command.add("-target"); command.add("1.8");
        command.add("-d"); command.add(classes.toString());
        command.add("-classpath"); command.add(joinClasspath(jars));
        for (Path source : listJavaFiles(src))
            command.add(source.toString());
        run(command, src.getParent());
    }

    private static List<Path> listJavaFiles(Path src) throws IOException {
        List<Path> result = new ArrayList<>();
        try (Stream<Path> files = Files.walk(src)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .forEach(result::add);
        }
        Collections.sort(result);
        return result;
    }

    private static String joinClasspath(List<Path> jars) {
        StringBuilder sb = new StringBuilder();
        for (Path jar : jars) {
            if (sb.length() > 0) sb.append(File.pathSeparatorChar);
            sb.append(jar.toString());
        }
        return sb.toString();
    }

    // =========================================================================
    // Fat JAR
    // =========================================================================

    /**
     * Crea un fat JAR che contiene: classi compilate, librerie e l'intera
     * cartella assets/. Includere assets nel JAR rende l'exe autosufficiente:
     * LibGDX cerca i file prima nel classpath (dentro il JAR) e quindi li
     * trova indipendentemente dalla directory di lavoro al momento dell'avvio.
     */
    private static void createFatJar(Path classes, List<Path> jars,
                                     Path assets, Path output) throws Exception {
        Files.createDirectories(output.getParent());

        Manifest manifest = new Manifest();
        Attributes attrs  = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.put(Attributes.Name.MAIN_CLASS, MAIN_CLASS);

        Set<String> written = new LinkedHashSet<>();
        try (JarOutputStream out = new JarOutputStream(
                new BufferedOutputStream(Files.newOutputStream(output)), manifest)) {
            written.add("META-INF/MANIFEST.MF");
            addDirectoryToJar(out, written, classes, classes);
            for (Path jar : jars)
                addJarToJar(out, written, jar);
            // Aggiunge assets/ nel JAR come "assets/..." così LibGDX li trova
            // via classpath lookup (Gdx.files.internal controlla il classpath prima
            // del filesystem → l'exe funziona da qualsiasi directory).
            if (Files.isDirectory(assets)) {
                System.out.println("Bundle assets in JAR: " + assets);
                addDirectoryToJar(out, written, assets.getParent(), assets);
            }
        }
    }

    private static void addDirectoryToJar(JarOutputStream out, Set<String> written,
                                          Path root, Path dir) throws IOException {
        try (Stream<Path> files = Files.walk(dir)) {
            List<Path> sorted = new ArrayList<>();
            files.filter(Files::isRegularFile).forEach(sorted::add);
            Collections.sort(sorted);
            for (Path file : sorted) {
                String name = root.relativize(file).toString().replace('\\', '/');
                if (written.add(name)) {
                    out.putNextEntry(new JarEntry(name));
                    Files.copy(file, out);
                    out.closeEntry();
                }
            }
        }
    }

    private static void addJarToJar(JarOutputStream out, Set<String> written,
                                    Path jarPath) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name    = entry.getName();
                if (entry.isDirectory() || skipJarEntry(name) || !written.add(name)) continue;
                out.putNextEntry(new JarEntry(name));
                try (InputStream in = jar.getInputStream(entry)) {
                    copy(in, out);
                }
                out.closeEntry();
            }
        }
    }

    private static boolean skipJarEntry(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        return upper.equals("META-INF/MANIFEST.MF")
                || upper.equals("META-INF/INDEX.LIST")
                || upper.equals("MODULE-INFO.CLASS")
                || (upper.startsWith("META-INF/")
                && (upper.endsWith(".SF") || upper.endsWith(".DSA") || upper.endsWith(".RSA")));
    }

    // =========================================================================
    // Icona ICO
    // =========================================================================

    /** Converte mask.png (qualsiasi dimensione) in un ICO 32×32 per Launch4j. */
    private static void createIco(Path png, Path ico) throws Exception {
        BufferedImage source = ImageIO.read(png.toFile());
        require(source != null, "Impossibile leggere l'immagine: " + png);

        // Ridimensiona a 32×32 con bicubica
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(source, 0, 0, 32, 32, null);
        g.dispose();

        // Costruisce il DIB (Device-Independent Bitmap) nel formato ICO
        ByteArrayOutputStream dib  = new ByteArrayOutputStream();
        DataOutputStream      data = new DataOutputStream(dib);
        writeInt(data, 40);          // biSize
        writeInt(data, 32);          // biWidth
        writeInt(data, 64);          // biHeight (doppio: XOR + AND mask)
        writeShort(data, 1);         // biPlanes
        writeShort(data, 32);        // biBitCount
        writeInt(data, 0);           // biCompression
        writeInt(data, 32 * 32 * 4); // biSizeImage
        writeInt(data, 0);           // biXPelsPerMeter
        writeInt(data, 0);           // biYPelsPerMeter
        writeInt(data, 0);           // biClrUsed
        writeInt(data, 0);           // biClrImportant
        // Pixel in ordine bottom-up, formato BGRA
        for (int y = 31; y >= 0; y--) {
            for (int x = 0; x < 32; x++) {
                int argb = image.getRGB(x, y);
                data.writeByte(argb & 0xFF);         // B
                data.writeByte((argb >> 8) & 0xFF);  // G
                data.writeByte((argb >> 16) & 0xFF); // R
                data.writeByte((argb >> 24) & 0xFF); // A
            }
        }
        data.write(new byte[32 * 4]); // AND mask (tutto trasparente)

        byte[] imageData = dib.toByteArray();

        Files.createDirectories(ico.getParent());
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(ico.toFile()))) {
            writeShort(out, 0);          // Reserved
            writeShort(out, 1);          // Type: ICO
            writeShort(out, 1);          // Numero di immagini
            out.writeByte(32);           // Width
            out.writeByte(32);           // Height
            out.writeByte(0);            // ColorCount
            out.writeByte(0);            // Reserved
            writeShort(out, 1);          // Planes
            writeShort(out, 32);         // BitCount
            writeInt(out, imageData.length); // SizeInBytes
            writeInt(out, 22);           // Offset dei dati (dopo l'header)
            out.write(imageData);
        }
    }

    // =========================================================================
    // Launch4j
    // =========================================================================

    /** Scarica e decomprime Launch4j se non è già presente nella directory tmp. */
    private static Path ensureLaunch4j(Path tmp) throws Exception {
        Path zip    = tmp.resolve("launch4j.zip");
        Path target = tmp.resolve("launch4j");
        Files.createDirectories(target);

        download(LAUNCH4J_URL, zip);
        unzip(zip, target);
        Files.deleteIfExists(zip);

        Path exe = findLaunch4j(target);
        require(exe != null, "launch4jc.exe non trovato nell'archivio scaricato.");
        return exe;
    }

    private static Path findLaunch4j(Path root) throws IOException {
        if (!Files.isDirectory(root)) return null;
        try (Stream<Path> files = Files.walk(root)) {
            List<Path> matches = new ArrayList<>();
            files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase("launch4jc.exe"))
                    .forEach(matches::add);
            Collections.sort(matches);
            return matches.isEmpty() ? null : matches.get(0);
        }
    }

    /** Scrive il file XML di configurazione per launch4jc. */
    /**
     * Scrive il file XML di configurazione per launch4jc.
     * Nota: launch4j accetta in <chdir> solo percorsi relativi all'exe,
     * quindi l'elemento viene omesso.
     */
    private static void writeLaunch4jConfig(Path config, Path jar, Path exe,
                                            Path ico) throws IOException {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<launch4jConfig>\n"
                        + "  <headerType>gui</headerType>\n"
                        + "  <jar>"     + slash(jar) + "</jar>\n"
                        + "  <outfile>" + slash(exe) + "</outfile>\n"
                        + "  <dontWrapJar>false</dontWrapJar>\n"
                        + "  <errTitle>HollowKnight-Pablo</errTitle>\n"
                        + "  <icon>"    + slash(ico) + "</icon>\n"
                        + "  <jre>\n"
                        + "    <minVersion>1.8.0</minVersion>\n"
                        + "    <jdkPreference>preferJre</jdkPreference>\n"
                        + "    <runtimeBits>64/32</runtimeBits>\n"
                        + "  </jre>\n"
                        + "</launch4jConfig>\n";
        Files.write(config, xml.getBytes(StandardCharsets.UTF_8));
    }

    // =========================================================================
    // Ricerca strumenti (javac, ecc.)
    // =========================================================================

    /**
     * Cerca l'eseguibile richiesto nell'ordine:
     *   1. JDK che sta eseguendo questo programma (java.home)
     *   2. Variabile d'ambiente JAVA_HOME
     *   3. Percorsi fissi di fallback su Windows
     */
    private static Path findTool(String exe) {
        List<Path> candidates = new ArrayList<>();

        // 1. JDK corrente — il modo più affidabile
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            // Su JDK 9+ java.home è già la root del JDK
            candidates.add(Paths.get(javaHome).resolve("bin").resolve(exe));
            // Su JDK 8 java.home punta a jre/, il JDK è nel parent
            Path parent = Paths.get(javaHome).getParent();
            if (parent != null)
                candidates.add(parent.resolve("bin").resolve(exe));
        }

        // 2. JAVA_HOME (variabile d'ambiente impostata manualmente)
        String javaHomeEnv = System.getenv("JAVA_HOME");
        if (javaHomeEnv != null)
            candidates.add(Paths.get(javaHomeEnv).resolve("bin").resolve(exe));

        // 3. Percorsi di fallback comuni su Windows
        candidates.add(Paths.get("C:/Program Files/Java/jdk-25.0.1/bin").resolve(exe));
        candidates.add(Paths.get("C:/Program Files/Java/jdk-1.8/bin").resolve(exe));

        for (Path path : candidates)
            if (Files.isRegularFile(path)) return path;

        throw new IllegalStateException(exe + " non trovato. Installare un JDK e impostare JAVA_HOME.");
    }

    // =========================================================================
    // Utility: esecuzione processo
    // =========================================================================

    private static void run(List<String> command, Path dir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(dir.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (InputStream in = process.getInputStream()) {
            copy(in, System.out);
        }
        int exit = process.waitFor();
        if (exit != 0)
            throw new IllegalStateException("Comando fallito (exit " + exit + "): " + command);
    }

    // =========================================================================
    // Utility: download e decompressione
    // =========================================================================

    private static void download(String url, Path dest) throws Exception {
        URL current = new URL(url);
        for (int i = 0; i < 8; i++) {
            HttpURLConnection conn = (HttpURLConnection) current.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "DownloadExe");
            int code = conn.getResponseCode();
            if (code >= 300 && code < 400) {
                current = new URL(current, conn.getHeaderField("Location"));
                continue;
            }
            require(code >= 200 && code < 300,
                    "Download Launch4j fallito: HTTP " + code);
            try (InputStream  in  = new BufferedInputStream(conn.getInputStream());
                 OutputStream out = new BufferedOutputStream(Files.newOutputStream(dest))) {
                copy(in, out);
            }
            return;
        }
        throw new IOException("Troppi redirect durante il download di Launch4j.");
    }

    private static void unzip(Path zip, Path dest) throws IOException {
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                Path out = dest.resolve(entry.getName()).normalize();
                if (!out.startsWith(dest.normalize()))
                    throw new IOException("Zip path traversal rilevato: " + entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                }
                in.closeEntry();
            }
        }
    }

    // =========================================================================
    // Utility: filesystem
    // =========================================================================

    private static void delete(Path path) throws IOException {
        if (!Files.exists(path)) return;
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static boolean hasMzHeader(Path exe) throws IOException {
        byte[] header = Files.readAllBytes(exe);
        return header.length > 2 && header[0] == 'M' && header[1] == 'Z';
    }

    // =========================================================================
    // Utility: I/O e bit
    // =========================================================================

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
    }

    /** Scrive un intero a 16 bit in little-endian. */
    private static void writeShort(DataOutputStream out, int value) throws IOException {
        out.writeByte(value & 0xFF);
        out.writeByte((value >>> 8) & 0xFF);
    }

    /** Scrive un intero a 32 bit in little-endian. */
    private static void writeInt(DataOutputStream out, int value) throws IOException {
        out.writeByte(value & 0xFF);
        out.writeByte((value >>> 8) & 0xFF);
        out.writeByte((value >>> 16) & 0xFF);
        out.writeByte((value >>> 24) & 0xFF);
    }

    /** Converte un Path in stringa con slash Unix (richiesto da Launch4j). */
    private static String slash(Path path) {
        return path.toAbsolutePath().normalize().toString().replace('\\', '/');
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}