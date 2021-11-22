package core;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JarToJre {


    private static File jarPath;
    private static File jreOut;
    private static JavaTools javaTools;
    private static File tempWorkFile;
    private static File tempJarFile;


    public static void main(String[] args) throws Exception {
        args = new String[]{
                "E:/git/github/MineStore/MineServer/target/MineServer-0.0.1-SNAPSHOT.jar"
        };

        initCommandLine(args);
        initJavaTools();
        initTempFile();

        work();
        release();
    }

    private static void release() {
        Util.cleanFile(tempWorkFile);
    }

    /**
     * 初始化命令行
     *
     * @param args
     */
    private static void initCommandLine(String[] args) {
        if (args.length < 1) {
            System.out.println("java JarToJre.java springboot.jar jre");
            return;
        }

        jarPath = new File(args[0]);
        jreOut = new File(args.length == 1 ? "jre" : args[1]);

        System.out.println("jar : " + jarPath.getAbsolutePath());
        System.out.println("out : " + jreOut.getAbsolutePath());

    }


    /**
     * 初始化java工具
     */
    private static void initJavaTools() {
        javaTools = new JavaTools();
    }


    /**
     * 初始化临时目录
     */
    private static void initTempFile() {
        tempWorkFile = new File(System.getProperty("java.io.tmpdir") + "/work_" + System.currentTimeMillis());
        tempJarFile = new File(tempWorkFile.getAbsolutePath() + "/" + jarPath.getName());
        System.out.println("tmp : " + tempWorkFile.getAbsolutePath());
        tempWorkFile.mkdirs();
    }

    /**
     * 开始任务
     */
    private static void work() throws Exception {
        //拷贝文件
        Util.copyFile(jarPath, tempJarFile);

        //解压文件
        Util.unzip(javaTools.getJarFile(), tempJarFile, tempWorkFile);

        //分析依赖
        String jdeps = Util.jdeps(javaTools.getJdepsFile(), tempWorkFile);

    }


    private static class JavaTools {

        private File javaHome = new File(System.getProperty("java.home"));


        public File getJdepsFile() {
            return getFile("jdeps");
        }

        public File getJlinkFile() {
            return getFile("jlink");
        }

        public File getJarFile() {
            return getFile("jar");
        }


        private File getFile(String appName) {
            File file = new File(javaHome.getAbsolutePath() + "/bin/" + appName);
            if (file.exists()) {
                return file;
            }
            file = new File(javaHome.getAbsolutePath() + "/bin/" + appName + ".exe");
            if (file.exists()) {
                return file;
            }
            return null;
        }
    }


    public static class Util {

        /**
         * 递归扫描所哟文件
         *
         * @param rootFile
         * @param files
         */
        public static void scanFile(File rootFile, List<File> files) {
            if (rootFile.isDirectory()) {
                for (File file : rootFile.listFiles()) {
                    scanFile(file, files);
                }
            } else if (rootFile.isFile()) {
                files.add(rootFile);
            }
        }


        /**
         * 分析这个目录下的所有java支持的文件的依赖
         *
         * @param workFile
         * @return
         */
        public static String jdeps(File jdkJarFile, File workFile) throws Exception {

            //扫描所有的文件
            List<File> files = new ArrayList<>();
            scanFile(workFile, files);
            List<String> targetFiles = files.stream()
                    .filter((it) -> {
                        String fileName = it.getName();
                        String extName = fileName.substring(fileName.lastIndexOf("."), fileName.length());
//                        return extName.equalsIgnoreCase(".class") || extName.equalsIgnoreCase(".jar");
                        return extName.equalsIgnoreCase(".jar");
                    }).map((it) -> {
                        return it.getAbsolutePath();
                    })
                    .collect(Collectors.toList());


            //执行命令行
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.directory(workFile);
            List<String> cmds = new ArrayList<>() {{
                add(jdkJarFile.getAbsolutePath());
                add("--module-path");
                add("$JAVA_HOME/jmods");
                add("--print-module-deps");
                add("--ignore-missing-deps");
                add("--multi-release 11");
            }};
            cmds.addAll(targetFiles);
            processBuilder.command(cmds);


            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

            Process process = processBuilder.start();
            process.waitFor();

//            String.join(" ", tarFile)

            // class, jar

//            jdeps --module-path $JAVA_HOME/jmods --print-module-deps --ignore-missing-deps  --multi-release 11 file1 file2

            return null;
        }


        /**
         * 解压jar包
         *
         * @param jdkJarFile
         * @param jarPackFile
         * @param workFile
         */

        public static void unzip(File jdkJarFile, File jarPackFile, File workFile) throws Exception {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.directory(workFile);
            processBuilder.command(jdkJarFile.getAbsolutePath(), "-xvf", jarPackFile.getAbsolutePath());

            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

            Process process = processBuilder.start();
            process.waitFor();


        }


        /**
         * 拷贝文件
         *
         * @param source
         * @param dest
         */
        public static void copyFile(File source, File dest) {
            FileChannel inputChannel = null;
            FileChannel outputChannel = null;
            try {
                inputChannel = new FileInputStream(source).getChannel();
                outputChannel = new FileOutputStream(dest).getChannel();
                outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    inputChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    outputChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        /**
         * 递归删除所有文件包含子文件目录
         *
         * @param file
         */
        public static void cleanFile(File file) {
            Arrays.stream(file.listFiles()).forEach((it) -> {
                if (it.isDirectory()) {
                    //递归删除
                    cleanFile(it);
                    it.delete();
                } else {
                    it.delete();
                }
            });
            file.delete();
        }


    }


}
