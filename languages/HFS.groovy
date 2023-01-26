@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.metadata.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import com.ibm.dbb.build.report.records.*
import com.ibm.dbb.build.report.*
import groovy.transform.*

// define script properties
@Field BuildProperties props = BuildProperties.getInstance()

@Field def buildUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BuildUtilities.groovy"))


println("** Processing files mapped to ${this.class.getName()}.groovy script")

List<String> buildList = argMap.buildList

// iterate through build list
buildList.each { buildFile ->
	println "*** Transferring file $buildFile"

	// local variables and log file
	String member = CopyToPDS.createMemberName(buildFile)
	// validate lenght of member name
	def memberLen = member.size()


	String absolutePath = buildUtils.getAbsolutePath(buildFile)
	println absolutePath
	
	File absoluteFile = new File("$absolutePath")
	if (absoluteFile.exists()){

		rootDir = absoluteFile.getParentFile().getParent()

		//create a new record of type AnyTypeRecord
		AnyTypeRecord ussRecord = new AnyTypeRecord("USS_RECORD")
		// set attributes
		ussRecord.setAttribute("file", buildFile)
		ussRecord.setAttribute("label", "MyTypeLabel")
		ussRecord.setAttribute("outputfile", absolutePath)

		// add new record to build report
		if(props.verbose) "* Adding USS_RECORD for $buildFile"
		BuildReportFactory.getBuildReport().addRecord(ussRecord)
		
	} else {
		println "$absolutePath does not exist."
	}
}

