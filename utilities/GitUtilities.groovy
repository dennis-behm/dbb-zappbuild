@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.repository.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*
import com.ibm.dbb.build.report.*
import com.ibm.dbb.build.report.records.*


/*
 * Tests if directory is in a local git repository
 *
 * @param  String dir  		Directory to test
 * @return boolean		
 */
def isGitDir(String dir) {
	String cmd = "git -C $dir rev-parse --is-inside-work-tree"
	StringBuffer gitResponse = new StringBuffer()
	StringBuffer gitError = new StringBuffer()
	boolean isGit = false

	Process process = cmd.execute()
	process.waitForProcessOutput(gitResponse, gitError)
	if (gitError) {
		println("*? Warning executing isGitDir($dir). Git command: $cmd error: $gitError")
	}
	else if (gitResponse) {
		isGit = gitResponse.toString().trim().toBoolean()
	}

	return isGit
}

/*
 * Returns the current Git branch
 *
 * @param  String gitDir  		Local Git repository directory
 * @return String gitBranch     The current Git branch
 */
def getCurrentGitBranch(String gitDir) {
	String cmd = "git -C $gitDir rev-parse --abbrev-ref HEAD"
	StringBuffer gitBranch = new StringBuffer()
	StringBuffer gitError = new StringBuffer()

	Process process = cmd.execute()
	process.waitForProcessOutput(gitBranch, gitError)
	if (gitError) {
		println("*! Error executing Git command: $cmd error: $gitError")
	}
	return gitBranch.toString().trim()
}

/*
 * Returns the current Git branch in detached HEAD state
 *
 * @param  String gitDir  		Local Git repository directory
 * @return String gitBranch     The current Git branch
 */
def getCurrentGitDetachedBranch(String gitDir) {
	String cmd = "git -C $gitDir show -s --pretty=%D HEAD"
	StringBuffer gitBranch = new StringBuffer()
	StringBuffer gitError = new StringBuffer()

	Process process = cmd.execute();
	process.waitForProcessOutput(gitBranch, gitError)
	if (gitError) {
		println("*! Error executing Git command: $cmd error: $gitError")
	}

	String gitBranchString = gitBranch.toString()
	def gitBranchArr = gitBranchString.split(',')
	def solution = ""
	for (i = 0; i < gitBranchArr.length; i++) {
		if (gitBranchArr[i].contains("/")) {
			solution = gitBranchArr[i].replaceAll(".*?/", "").trim()
		}
	}

	return (solution != "") ? solution : println("*! Error parsing branch name: $gitBranch")
}

/*
 * Returns true if this is a detached HEAD
 *
 * @param  String gitDir  		Local Git repository directory
 */
def isGitDetachedHEAD(String gitDir) {
	String cmd = "git -C $gitDir status"
	StringBuffer gitStatus = new StringBuffer()
	StringBuffer gitError = new StringBuffer()

	Process process = cmd.execute()
	process.waitForProcessOutput(gitStatus, gitError)
	if (gitError) {
		println("*! Error executing Git command: $cmd error $gitError")
	}

	return gitStatus.toString().contains("HEAD detached at")
}

/*
 * Returns the current Git hash
 *
 * @param  String gitDir  		Local Git repository directory
 * @return String gitHash       The current Git hash
 */
def getCurrentGitHash(String gitDir) {
	String cmd = "git -C $gitDir rev-parse HEAD"
	StringBuffer gitHash = new StringBuffer()
	StringBuffer gitError = new StringBuffer()

	Process process = cmd.execute()
	process.waitForProcessOutput(gitHash, gitError)
	if (gitError) {
		print("*! Error executing Git command: $cmd error: $gitError")
	}
	return gitHash.toString().trim()
}

/*
 * Returns the current Git hash for this file path
 *
 * @param  String gitDir  		Local Git repository directory
 * @param  String filePath		filePath relative to gitDir
 * @return String gitHash       The current Git hash
 */
def getFileCurrentGitHash(String gitDir, String filePath) {
	String cmd = "git -C $gitDir rev-list -1 HEAD " + filePath
	StringBuffer gitHash = new StringBuffer()
	StringBuffer gitError = new StringBuffer()

	Process process = cmd.execute()
	process.waitForProcessOutput(gitHash, gitError)
	if (gitError) {
		print("*! Error executing Git command: $cmd error: $gitError")
	}
	return gitHash.toString().trim()
}

/*
 * Returns the current Git url
 *
 * @param  String gitDir  		Local Git repository directory
 * @return String gitUrl       The current Git url
 */
def getCurrentGitUrl(String gitDir) {
	String cmd = "git -C $gitDir config --get remote.origin.url"
	StringBuffer gitUrl = new StringBuffer()
	StringBuffer gitError = new StringBuffer()

	Process process = cmd.execute()
	process.waitForProcessOutput(gitUrl, gitError)

	if (gitError) {
		print("*! Error executing Git command: $cmd error: $gitError")
	}
	return gitUrl.toString().trim()
}


/*
 * Returns the lst previous Git commit hash
 * 
 * @param String gitDir       Local Git repository directory
 * @return String gitHash     The previous Git commit hash
 */
def getPreviousGitHash(String gitDir) {
	String cmd = "git -C $gitDir --no-pager log -n 1 --skip=1"
	StringBuffer gitStdout = new StringBuffer()
	StringBuffer gitError = new StringBuffer()

	Process process = cmd.execute()
	process.waitForProcessOutput(gitStdout, gitError)
	if (gitError) {
		print("*! Error executing Git command: $cmd error: $gitError")
	}
	else {
		return gitStdout.toString().minus('commit').trim().split()[0]
	}
}

def getChangedFiles(String gitDir, String baseHash, String currentHash) {
	String cmd = "git -C $gitDir --no-pager diff --name-status $baseHash $currentHash"
	def git_diff = new StringBuffer()
	def git_error = new StringBuffer()
	def changedFiles = []
	def deletedFiles = []

	def process = cmd.execute()
	process.waitForProcessOutput(git_diff, git_error)

	// handle command error
	if (git_error.size() > 0) {
		println("*! Error executing Git command: $cmd error: $git_error")
		println ("*! Attempting to parse unstable git command for changed files...")
	}

	for (line in git_diff.toString().split("\n")) {
		// process files from git diff
		try {
			action = line.split()[0]
			file = line.split()[1]
			// handle deleted files
			if (action == "D") {
				deletedFiles.add(file)
			}
			// handle changed files
			else {
				changedFiles.add(file)
			}
		}
		catch (Exception e) {
			// no changes or unhandled format
		}
	}

	return [changedFiles, deletedFiles]
}

def getCurrentChangedFiles(String gitDir, String currentHash, String verbose) {
	if (verbose) println "** Running git command: git -C $gitDir show --pretty=format: --name-status $currentHash"
	String cmd = "git -C $gitDir show --pretty=format: --name-status $currentHash"
	def gitDiff = new StringBuffer()
	def gitError = new StringBuffer()
	def changedFiles = []
	def deletedFiles = []

	Process process = cmd.execute()
	process.waitForProcessOutput(gitDiff, gitError)

	// handle command error
	if (gitError.size() > 0) {
		println("*! Error executing Git command: $cmd error: $gitError")
		println ("*! Attempting to parse unstable git command for changed files...")
	}

	for (line in gitDiff.toString().split("\n")) {
		if (verbose) println "** Git command line: $line"
		// process files from git diff
		try {
			action = line.split()[0]
			file = line.split()[1]
			// handle deleted files
			if (action == "D") {
				deletedFiles.add(file)
			}
			// handle changed files
			else {
				changedFiles.add(file)
			}
		}
		catch (Exception e) {
			// no changes or unhandled format
		}
	}

	return [changedFiles, deletedFiles]
}

// Inspect git log to retrieve hash - file - tag mapping

def getModifiedFiles(String gitDir, String featureBranchName) {
	// git --no-pager log --oneline --merges
	// git --no-pager log --oneline --merges 6780aab1d08d1f658479d01eb02ce88964822d1d...f741490479655b34f0ffd38b7371959241af0184
	// 	f741490 (HEAD, tag: wi110.01, origin/master, refs/pipelines/694, refs/pipelines/693) Merge branch '5-wi110' into 'master'
	// 	1b56a12 (tag: wi100.01, refs/pipelines/692) Merge branch '4-wi100' into 'master'

	Set<String> modifiedFiles = new HashSet<String>()
	def changedFiles = []
	def deletedFiles = []

	
	String cmd = "git -C $gitDir --no-pager log --oneline --merges --format=%H;%D;%s"
	def git_log = new StringBuffer()
	def git_error = new StringBuffer()

	Process process = cmd.execute()
	process.waitForProcessOutput(git_log, git_error)

	// handle command error
	if (git_error.size() > 0) {
		println("*! Error executing Git command: $cmd error: $git_error")
	}

	for (line in git_log.toString().split("\n")) {
		// process files from git diff
		//println("Line: $line")
		try {


			attributes = line.split(";")
			hash = attributes[0]
			references = attributes[1]
			msg = attributes[2]

			//println("hash: $hash")
			//println("ref : $references")
			//println("msg : $msg")

			// getTag
			refs = references.split(",")
			refs.each{ ref ->
				if (ref.contains("tag:")){
					def tag = ref.split("tag:")[1]
					//println("! gittag : " + tag)
				}
			}

			// hash
			//println("! $hash")

			if (msg.contains(featureBranchName)){
				(changedFiles,deletedFiles) = getChangedFilesMergeCommit(gitDir, hash)
				changedFiles.each{ file ->
					//println("!!! $hash --->  $file ")
					modifiedFiles.add(file)
				}
			}
		}
		catch (Exception e) {
			println e
		}
	}

	return modifiedFiles

}

def getChangedFilesMergeCommit(String gitDir, String hash) {
	//println "** Running git command: git -C $gitDir show --first-parent --pretty=format: --name-status $hash"
	String cmd = "git -C $gitDir show --first-parent --pretty=format: --name-status $hash"
	def gitDiff = new StringBuffer()
	def gitError = new StringBuffer()
	Set<String> changedFiles = new HashSet<String>()
	Set<String> deletedFiles = new HashSet<String>()

	Process process = cmd.execute()
	process.waitForProcessOutput(gitDiff, gitError)

	// handle command error
	if (gitError.size() > 0) {
		println("*! Error executing Git command: $cmd error: $gitError")
		println ("*! Attempting to parse unstable git command for changed files...")
	}

	for (line in gitDiff.toString().split("\n")) {
		//println "** Git command line: $line"
		// process files from git diff
		try {
			action = line.split()[0]
			file = line.split()[1]
			// handle deleted files
			if (action == "D") {
				deletedFiles.add(file)
			}
			// handle changed files
			else {
				changedFiles.add(file)
			}
		}
		catch (Exception e) {
			// no changes or unhandled format
		}
	}

	return [changedFiles, deletedFiles]
}

