@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*
import com.ibm.dbb.build.report.*
import com.ibm.dbb.build.report.records.*
import com.ibm.jzos.ZFile
import com.ibm.dbb.metadata.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*
import com.ibm.dbb.build.report.*
import com.ibm.dbb.build.report.records.*
import groovy.yaml.YamlSlurper
import groovy.yaml.YamlBuilder
import groovy.lang.GroovyShell
import groovy.util.*
import java.nio.file.*

// define script properties
@Field BuildProperties props = BuildProperties.getInstance()

@Field ArrayList<RequiredBuildDataset> requiredBuildDatasets = new ArrayList()
@Field Language lang = new Language()

class LanguageConfiguration {
	ArrayList<Language> tasks
}

class Language {
	String language
	ArrayList<String> sources
	ArrayList<RequiredBuildDataset> datasets
	ArrayList<Step> steps
}

class RequiredBuildDataset {
	String name
	String options
}

class Step {
	String step
	String type
	String pgm
	String parm
	String maxRC
	ArrayList<String> dds
}

class CopyStep {
	String step = "copySrc"
	String type = "copy"
	String source = "WORKSPACE/FILE"
	String target
	DependencySearch searchConfig
}

class DependencySearch {
	String search
	ArrayList<DependendeSearchMappingEntry> mappings
}

class DependendeSearchMappingEntry{
	String source
	String dataset
}

// ScriptName - name
// langPrefix - build datasets
// searchPath - upload
// set of MVSExecs
def generateLanguageConfigurationYaml(String langName, String langPrefix, String searchPath, ArrayList<MVSExec> mvsExecSet) {
	
	// init language
	lang.language=langName
	lang.datasets=new ArrayList<RequiredBuildDataset>()
	lang.steps = new ArrayList<Step>()
	
	lang.sources = ["**.${langPrefix}"]
	
	// add required build datasets
	generateRequiredBuildDatasetDefinitions(langPrefix)
	
	// add default copy step
	
	CopyStep copyStep = new CopyStep()
	copyStep.target = "<todo>"
	DependencySearch searchConfig = new DependencySearch()
	searchConfig.search = searchPath
	DependendeSearchMappingEntry searchPathMapping = new DependendeSearchMappingEntry()
	searchPathMapping.source = "**/*"
	searchPathMapping.dataset = "COPY"
	copyStep.searchConfig = searchConfig
	
	lang.steps.add(copyStep)
	
	// add steps
	int stepNumber = 1
	mvsExecSet.each{mvsExec ->
		Step langStep = convertMVSExecToStepDefintion(mvsExec, "step" + stepNumber++)
		lang.steps.add(langStep)
	}
		
	//writeLanguageYaml
	
	def yamlBuilder = new YamlBuilder()
	yamlBuilder(lang)
	println yamlBuilder.toString()
	
	File yamlFile = new File("${langName}.yaml")
	yamlFile.withWriter() { writer ->
		writer.write(yamlBuilder.toString())
	}
}

def generateRequiredBuildDatasetDefinitions(String langPrefix) {
	
	if (props."${langPrefix}_srcDatasets") {
		props."${langPrefix}_srcDatasets".split(',').each{ ds ->
			RequiredBuildDataset buildDataset = new RequiredBuildDataset()
			buildDataset.name=ds.replaceAll(props.hlq,"_HLQ_").replaceAll(props.hlq,"_HLQ_")
			buildDataset.options=props."${langPrefix}_srcOptions"
			lang.datasets.add(buildDataset)
		}
	}
	
	if (props."${langPrefix}_loadDatasets") {
		props."${langPrefix}_loadDatasets".split(',').each{ ds ->
			RequiredBuildDataset buildDataset = new RequiredBuildDataset()
			buildDataset.name=ds.replaceAll(props.hlq,"_HLQ_")
			buildDataset.options=props."${langPrefix}_loadOptions"
			lang.datasets.add(buildDataset)
		}
	}
	
	if (props."${langPrefix}_reportDatasets") {
		props."${langPrefix}_reportDatasets".split(',').each{ ds ->
			RequiredBuildDataset buildDataset = new RequiredBuildDataset()
			buildDataset.name=ds.replaceAll(props.hlq,"_HLQ_")
			buildDataset.options=props."${langPrefix}_reportOptions"
			lang.datasets.add(buildDataset)
		}
	}
	
	if (props."${langPrefix}_cexecDatasets") {
		props."${langPrefix}_cexecDatasets".split(',').each{ ds ->
			RequiredBuildDataset buildDataset = new RequiredBuildDataset()
			buildDataset.name=ds.replaceAll(props.hlq,"_HLQ_")
			buildDataset.options=props."${langPrefix}_cexecOptions"
			lang.datasets.add(buildDataset)
		}
	}
}

def convertMVSExecToStepDefintion(MVSExec mvsExec, String stepName) {
	
	Step step = new Step()
	step.step=stepName
	step.type="mvs"
	step.pgm=mvsExec.pgm
	step.parm=mvsExec.parm
	step.maxRC="0"
	step.dds = new ArrayList()
	mvsExec.getDDStatements().each { it ->
		def ddMsg = ""
		ddMsg = (it.name) ? "name: ${it.name}" : "${ddMsg}"
		ddMsg = (it.dsn) ? "${ddMsg}, dsn: " + (it.dsn).replaceAll(props.hlq,"_HLQ_") : "${ddMsg}"
		ddMsg = (it.options) ? "${ddMsg}, options: ${it.options}" : "${ddMsg}"
		ddMsg = (it.output) ? "${ddMsg}, output: ${it.output}" : "${ddMsg}"
		ddMsg = (it.deployType) ? "${ddMsg}, deployType: ${it.deployType}" : "${ddMsg}"
		
		step.dds.add("{ ${ddMsg} }")
		it.getConcatenations().each{ concatenation ->
			ddMsg = " "
			//ddMsg = (concatenation.name) ? "name: ${concatenation.name}" : "${ddMsg}"
			ddMsg = (concatenation.dsn) ? "${ddMsg}              dsn: " + concatenation.dsn.replaceAll(props.hlq,"_HLQ_") : "${ddMsg}"
			ddMsg = (concatenation.options) ? "${ddMsg}, options: ${concatenation.options}" : "${ddMsg}"
			step.dds.add("{ ${ddMsg} }")
		}
			
	}
	return step
}