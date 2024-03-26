@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import groovy.util.*

String[] cliArgs;
cliArgs = ["--generate"]
cliArgs = cliArgs + args

println cliArgs

runScript(new File("build.groovy"), cliArgs)