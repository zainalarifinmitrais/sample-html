import groovy.json.JsonSlurperClassic


node {
    env.AWS_DEFAULT_REGION = 'ap-southeast-1'
	
	String trainerName = 'fajar'

	//Cleanup workspace
	deleteDir()

	stage('Checkout') {
		checkout scm	
	}
	
	
	stage("Sonar Analyze") {
		def scannerHome = tool 'default';
	    withSonarQubeEnv('default') {
	      sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectKey=${trainerName} -Dsonar.sources=app"
	    }
	}
	

	stage('Deploy') {
		withCredentials([[$class          : 'UsernamePasswordMultiBinding',
                      credentialsId   : 'CdcAWS',
                      usernameVariable: 'AWS_ACCESS_KEY_ID',
                      passwordVariable: 'AWS_SECRET_ACCESS_KEY']]) {
	        
	        

	        //Zip artifact
	        sh("zip -r ${env.JOB_NAME}.zip .")
	        
	        //Upload artifact to S3
	        sh("aws s3 cp ${env.JOB_NAME}.zip s3://deployment-cdc/${env.JOB_NAME}.zip")

	        //Create Deployment
	        def result = sh(returnStdout:true, script: "aws deploy create-deployment --application-name CDC-deploy --deployment-group-name ${trainerName} --s3-location bucket=deployment-cdc,bundleType=zip,key=${env.JOB_NAME}.zip")	        
	        def json = parseJson(result)
	        String deploymentId = json.deploymentId

	        //Wait until success
	        sh("aws deploy wait deployment-successful --deployment-id ${deploymentId}")
	    }
	}	
}

def parseJson(String json) {
	return new JsonSlurperClassic().parseText(json)
}
