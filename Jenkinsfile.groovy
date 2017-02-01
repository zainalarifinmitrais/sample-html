//Codedeploy Config
import static java.util.UUID.randomUUID 


node {
    env.AWS_DEFAULT_REGION = 'ap-southeast-1'
	
	String stackName = 'cdc-fajar'

	deleteDir()
	stage('Checkout') {
		checkout scm	
	}
	
	
	stage("Sonar Analyze") {

	}
	//deploy
	stage('Deploy') {
		withCredentials([[$class          : 'UsernamePasswordMultiBinding',
                      credentialsId   : 'CdcAWS',
                      usernameVariable: 'AWS_ACCESS_KEY_ID',
                      passwordVariable: 'AWS_SECRET_ACCESS_KEY']]) {
	        
	        //ZIP
	        sh("zip -r ${applicationId}.zip .")
	        
	        //generate random string
	        String applicationId = randomUUID()

	        //Upload
	        sh("aws s3 cp app.zip s3://deployment-cdc/${applicationId}.zip")

	        //Create Deployment
	        sh("deploy create-deployment --application-name CDC-deploy --deployment-group-name ${stackName} --s3-location bucket=deployment-cdc,bundleType=zip,key=${applicationId}.zip > .deployment_id")
	        String deploymentId = getDeploymentId('.deployment_id')

	        //Wait until success
	        sh("aws deploy wait deployment-successful --deployment-id ${deploymentId}")
	    }
	}	
}

def getDeploymentId(fileName) {
    def matcher = readFile(fileName) =~ 'deploymentId\":\\s\"(.*)\"'
    matcher ? matcher[0][1] : null
}