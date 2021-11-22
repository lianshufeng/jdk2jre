package core;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class JarToJre {


    private static File jarPath;
    private static File jreOut;
    private static JavaTools javaTools;
    private static File tempWorkFile;
    private static File tempJarFile;


    public static void main(String[] args) throws Exception {
        args = new String[]{
                "E:\\git\\github\\MineStore\\MineClient\\target\\MineClient-0.0.1-SNAPSHOT.jar",
                "jre"
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
        if (args.length < 2) {
            System.out.println("java JarToJre.java springboot.jar jre");
            return;
        }
        jarPath = new File(args[0]);
        jreOut = new File(args[1]);
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
         * 分析这个目录下的所有java支持的文件的依赖
         *
         * @param work
         * @return
         */
        public static String jdeps(File work) {
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
