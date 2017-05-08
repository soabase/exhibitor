/*
 * If running in Jetty launch webpack dev server through npm,
 * otherwise then just compile frontend with webpack.
 */
boolean isJettyRun = false
List<String> goals = session.getGoals()
for (String s : goals)
    if (s.equals('jetty:run'))
        isJettyRun = true

if (isJettyRun) {
    println "Running command: npm run dev"
    new ProcessBuilder(["node/npm", "run", "dev"])
            .inheritIO()
            .directory(project.getBasedir())
            .start()
} else {
    println "Compiling client code..."
    def webpack = new ProcessBuilder(
            ["node/node", "node_modules/webpack/bin/webpack.js",
             "--progress",
             "--bail",
             "--config", "webpack.config.production.js",
             "-p"])
            .inheritIO().directory(project.getBasedir())
    def env = webpack.environment()
    env.put("WAR_NAME", project.build.finalName)
    def proc_webpack = webpack.start()
    proc_webpack.waitForOrKill(120000)
    if (proc_webpack.exitValue() != 0)
        throw new org.apache.maven.plugin.MojoFailureException("Error compiling client code")
}