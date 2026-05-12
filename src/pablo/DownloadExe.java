// DownloadExe.java

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
    private static final String MAIN_CLASS = "pablo.Launcher";
    private static final String EXE_NAME = "Pablo.exe";
    private static final String ICON = "assets/mask.png";
    private static final String LAUNCH4J_URL =
            "https://downloads.sourceforge.net/project/launch4j/launch4j-3/3.14/launch4j-3.14-win32.zip";

    public static void main(String[] args) throws Exception {
        Path project = findProjectRoot();
        Path src = project.resolve("src");
        Path dist = desktopDirectory();
        Path tmp = Files.createTempDirectory("PabloExeBuilder-");

        try {
            Path classes = tmp.resolve("classes");
            Path jar = tmp.resolve("Pablo.jar");
            Path ico = tmp.resolve("mask.ico");

            System.out.println("Project: " + project);
            require(Files.isRegularFile(src.resolve("pablo").resolve("Launcher.java")),
                    "Launcher.java non trovato.");
            require(Files.isRegularFile(project.resolve(ICON)), "Icona non trovata: " + ICON);

            List<Path> jars = findDependencyJars(project);
            require(!jars.isEmpty(), "Nessuna libreria trovata. Controllare .idea/libraries/GdxLibs.xml.");

            Files.deleteIfExists(dist.resolve(EXE_NAME));
            Files.createDirectories(classes);
            compileProject(src, classes, jars);
            createFatJar(classes, jars, jar);
            createIco(project.resolve(ICON), ico);

            Path launch4j = ensureLaunch4j(tmp);
            Path config = tmp.resolve("launch4j.xml");
            writeLaunch4jConfig(config, jar, dist.resolve(EXE_NAME), ico, project);
            run(Arrays.asList(launch4j.toString(), config.toString()), project);

            require(Files.isRegularFile(dist.resolve(EXE_NAME)), "Pablo.exe non creato.");
            require(hasMzHeader(dist.resolve(EXE_NAME)), "Pablo.exe non e' un eseguibile Windows valido.");
            System.out.println("Creato: " + dist.resolve(EXE_NAME));
        } finally {
            delete(tmp);
        }
    }

    private static Path desktopDirectory() {
        Path desktop = Paths.get(System.getProperty("user.home"), "Desktop");
        if (Files.isDirectory(desktop)) return desktop.toAbsolutePath().normalize();

        Path oneDriveDesktop = Paths.get(System.getProperty("user.home"), "OneDrive", "Desktop");
        if (Files.isDirectory(oneDriveDesktop)) return oneDriveDesktop.toAbsolutePath().normalize();

        throw new IllegalStateException("Desktop non trovato per l'utente corrente.");
    }

    private static Path findProjectRoot() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve("src"))
                    && Files.isDirectory(current.resolve("assets"))
                    && Files.isRegularFile(current.resolve("src").resolve("pablo").resolve("Launcher.java"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Eseguire DownloadExe dalla root del progetto HollowKnight-Pablo.");
    }

    private static List<Path> findDependencyJars(Path project) throws Exception {
        LinkedHashSet<Path> directories = new LinkedHashSet<Path>();
        Path ideaLibs = project.resolve(".idea").resolve("libraries");
        if (Files.isDirectory(ideaLibs)) {
            try (Stream<Path> files = Files.list(ideaLibs)) {
                files.filter(path -> path.getFileName().toString().endsWith(".xml"))
                        .forEach(path -> readLibraryXml(project, path, directories));
            }
        }

        directories.add(project.resolve("GdxLibs"));
        directories.add(project.resolve("lib"));
        directories.add(project.resolve("libs"));
        Path current = project;
        while (current != null) {
            directories.add(current.resolve("GdxLibs"));
            current = current.getParent();
        }
        String home = System.getProperty("user.home");
        if (home != null) directories.add(Paths.get(home).resolve("Documents").resolve("GdxLibs"));

        LinkedHashSet<Path> result = new LinkedHashSet<Path>();
        for (Path dir : directories) {
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(Files::isRegularFile)
                        .filter(DownloadExe::isRuntimeJar)
                        .forEach(path -> result.add(path.toAbsolutePath().normalize()));
            }
        }
        List<Path> sorted = new ArrayList<Path>(result);
        Collections.sort(sorted);
        return sorted;
    }

    private static void readLibraryXml(Path project, Path xml, Set<Path> directories) {
        try {
            String text = new String(Files.readAllBytes(xml), StandardCharsets.UTF_8);
            Matcher matcher = Pattern.compile("url=\"file://([^\"]+)\"").matcher(text);
            while (matcher.find()) {
                String raw = URLDecoder.decode(matcher.group(1), "UTF-8")
                        .replace("$PROJECT_DIR$", project.toString().replace('\\', '/'));
                if (raw.startsWith("/") && raw.length() > 2 && raw.charAt(2) == ':') {
                    raw = raw.substring(1);
                }
                if (!raw.toLowerCase(Locale.ROOT).endsWith(".jar")) {
                    directories.add(Paths.get(raw).toAbsolutePath().normalize());
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean isRuntimeJar(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".jar")
                && !name.contains("sources")
                && !name.contains("source")
                && !name.contains("javadoc");
    }

    private static void compileProject(Path src, Path classes, List<Path> jars) throws Exception {
        List<String> command = new ArrayList<String>();
        command.add(findTool("javac.exe").toString());
        command.add("-encoding");
        command.add("UTF-8");
        command.add("-source");
        command.add("1.8");
        command.add("-target");
        command.add("1.8");
        command.add("-d");
        command.add(classes.toString());
        command.add("-classpath");
        command.add(joinClasspath(jars));
        for (Path source : listJavaFiles(src)) {
            command.add(source.toString());
        }
        run(command, src.getParent());
    }

    private static List<Path> listJavaFiles(Path src) throws IOException {
        List<Path> result = new ArrayList<Path>();
        try (Stream<Path> files = Files.walk(src)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .forEach(result::add);
        }
        Collections.sort(result);
        return result;
    }

    private static String joinClasspath(List<Path> jars) {
        StringBuilder result = new StringBuilder();
        for (Path jar : jars) {
            if (result.length() > 0) result.append(File.pathSeparatorChar);
            result.append(jar.toString());
        }
        return result.toString();
    }

    private static void createFatJar(Path classes, List<Path> jars, Path output) throws Exception {
        Files.createDirectories(output.getParent());
        Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.put(Attributes.Name.MAIN_CLASS, MAIN_CLASS);

        Set<String> written = new LinkedHashSet<String>();
        try (JarOutputStream out = new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(output)), manifest)) {
            written.add("META-INF/MANIFEST.MF");
            addDirectoryToJar(out, written, classes, classes);
            for (Path jar : jars) addJarToJar(out, written, jar);
        }
    }

    private static void addDirectoryToJar(JarOutputStream out, Set<String> written, Path root, Path dir) throws IOException {
        try (Stream<Path> files = Files.walk(dir)) {
            List<Path> sorted = new ArrayList<Path>();
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

    private static void addJarToJar(JarOutputStream out, Set<String> written, Path jarPath) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
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

    private static void createIco(Path png, Path ico) throws Exception {
        BufferedImage source = ImageIO.read(png.toFile());
        require(source != null, "Impossibile leggere " + png);
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(source, 0, 0, 32, 32, null);
        g.dispose();

        ByteArrayOutputStream dib = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(dib);
        writeInt(data, 40);
        writeInt(data, 32);
        writeInt(data, 64);
        writeShort(data, 1);
        writeShort(data, 32);
        writeInt(data, 0);
        writeInt(data, 32 * 32 * 4);
        writeInt(data, 0);
        writeInt(data, 0);
        writeInt(data, 0);
        writeInt(data, 0);
        for (int y = 31; y >= 0; y--) {
            for (int x = 0; x < 32; x++) {
                int argb = image.getRGB(x, y);
                data.writeByte(argb & 0xFF);
                data.writeByte((argb >> 8) & 0xFF);
                data.writeByte((argb >> 16) & 0xFF);
                data.writeByte((argb >> 24) & 0xFF);
            }
        }
        data.write(new byte[32 * 4]);

        byte[] imageData = dib.toByteArray();
        Files.createDirectories(ico.getParent());
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(ico.toFile()))) {
            writeShort(out, 0);
            writeShort(out, 1);
            writeShort(out, 1);
            out.writeByte(32);
            out.writeByte(32);
            out.writeByte(0);
            out.writeByte(0);
            writeShort(out, 1);
            writeShort(out, 32);
            writeInt(out, imageData.length);
            writeInt(out, 22);
            out.write(imageData);
        }
    }

    private static Path ensureLaunch4j(Path tmp) throws Exception {
        Path zip = tmp.resolve("launch4j.zip");
        Path target = tmp.resolve("launch4j");
        Files.createDirectories(target);
        download(LAUNCH4J_URL, zip);
        unzip(zip, target);
        Files.deleteIfExists(zip);

        Path downloaded = findLaunch4j(target);
        require(downloaded != null, "launch4jc.exe non trovato.");
        return downloaded;
    }

    private static Path findLaunch4j(Path root) throws IOException {
        if (!Files.isDirectory(root)) return null;
        try (Stream<Path> files = Files.walk(root)) {
            List<Path> matches = new ArrayList<Path>();
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase("launch4jc.exe"))
                    .forEach(matches::add);
            Collections.sort(matches);
            return matches.isEmpty() ? null : matches.get(0);
        }
    }

    private static void writeLaunch4jConfig(Path config, Path jar, Path exe, Path ico, Path project) throws IOException {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<launch4jConfig>\n" +
                "  <headerType>gui</headerType>\n" +
                "  <jar>" + slash(jar) + "</jar>\n" +
                "  <outfile>" + slash(exe) + "</outfile>\n" +
                "  <dontWrapJar>false</dontWrapJar>\n" +
                "  <errTitle>HollowKnight-Pablo</errTitle>\n" +
                "  <chdir>" + slash(project) + "</chdir>\n" +
                "  <icon>" + slash(ico) + "</icon>\n" +
                "  <jre>\n" +
                "    <minVersion>1.8.0</minVersion>\n" +
                "    <jdkPreference>preferJre</jdkPreference>\n" +
                "    <runtimeBits>64/32</runtimeBits>\n" +
                "  </jre>\n" +
                "</launch4jConfig>\n";
        Files.write(config, xml.getBytes(StandardCharsets.UTF_8));
    }

    private static String slash(Path path) {
        return path.toAbsolutePath().normalize().toString().replace('\\', '/');
    }

    private static Path findJava8Runtime() {
        List<Path> candidates = new ArrayList<Path>();
        candidates.add(Paths.get("C:/Program Files/Java/jdk-1.8/jre"));
        candidates.add(Paths.get("C:/Program Files/Java/jre-1.8"));
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) candidates.add(Paths.get(javaHome));
        for (Path path : candidates) {
            if (Files.isRegularFile(path.resolve("bin").resolve("java.exe"))
                    && Files.isDirectory(path.resolve("lib"))) {
                return path;
            }
        }
        return null;
    }

    private static boolean enoughSpace(Path dist, Path runtime, Path assets, Path jar) {
        try {
            long needed = size(runtime) + size(assets) + Files.size(jar) * 2 + 200L * 1024L * 1024L;
            return dist.toAbsolutePath().getParent().toFile().getUsableSpace() > needed;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Path findTool(String exe) {
        List<Path> candidates = new ArrayList<Path>();
        candidates.add(Paths.get("C:/Program Files/Java/jdk-1.8/bin").resolve(exe));
        candidates.add(Paths.get("C:/Program Files/Java/jdk-25.0.1/bin").resolve(exe));
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            candidates.add(Paths.get(javaHome).resolve("bin").resolve(exe));
            Path parent = Paths.get(javaHome).getParent();
            if (parent != null) candidates.add(parent.resolve("bin").resolve(exe));
        }
        for (Path path : candidates) if (Files.isRegularFile(path)) return path;
        throw new IllegalStateException(exe + " non trovato. Installare un JDK.");
    }

    private static void run(List<String> command, Path dir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(dir.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (InputStream in = process.getInputStream()) {
            copy(in, System.out);
        }
        int exit = process.waitFor();
        if (exit != 0) throw new IllegalStateException("Comando fallito: " + command);
    }

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
            require(code >= 200 && code < 300, "Download Launch4j fallito: HTTP " + code);
            try (InputStream in = new BufferedInputStream(conn.getInputStream());
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
                if (!out.startsWith(dest.normalize())) throw new IOException("Zip non sicuro.");
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

    private static void copyDirectory(Path from, Path to) throws IOException {
        if (!Files.isDirectory(from)) return;
        Files.walkFileTree(from, new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(to.resolve(from.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path out = to.resolve(from.relativize(file));
                Files.createDirectories(out.getParent());
                Files.copy(file, out, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static long size(Path path) throws IOException {
        if (!Files.exists(path)) return 0;
        final long[] total = {0};
        try (Stream<Path> files = Files.walk(path)) {
            files.filter(Files::isRegularFile).forEach(file -> {
                try {
                    total[0] += Files.size(file);
                } catch (IOException ignored) {
                }
            });
        }
        return total[0];
    }

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

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
    }

    private static void writeShort(DataOutputStream out, int value) throws IOException {
        out.writeByte(value & 0xFF);
        out.writeByte((value >>> 8) & 0xFF);
    }

    private static void writeInt(DataOutputStream out, int value) throws IOException {
        out.writeByte(value & 0xFF);
        out.writeByte((value >>> 8) & 0xFF);
        out.writeByte((value >>> 16) & 0xFF);
        out.writeByte((value >>> 24) & 0xFF);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
