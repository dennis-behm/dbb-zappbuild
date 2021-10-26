pipeline {
	agent  { label "ztec-201-STC" }
	options { skipDefaultCheckout(true) }
	stages {
		stage('init') {
			steps {
				sh "echo 'Hello World'"
				sh "ls -lisa $HOME"
				sh "ls -lisa $GIT_SHELL"
				checkout scm
			}
		}
	}
}
