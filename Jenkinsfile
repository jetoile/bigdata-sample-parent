#!/usr/bin/env/groovy

node ('master') {
	stage 'Build'
	checkout scm

	def mvnHome = tool name: 'maven3.5.2', type: 'maven'
	def javaHome = tool name: 'jdk8', type: 'jdk'
	env.PATH = "${javaHome}/bin:${mvnHome}/bin:${env.PATH}"

	sh 'mvn install'

}

