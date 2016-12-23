/**
 *      Forked From: https://github.com/sbdobrescu/SmartThings/blob/master/Denon_AVR.groovy
 *  	Denon Network Receiver 
 *    	Based on Denon/Marantz receiver by Kristopher Kubicki
 *    	SmartThings driver to connect your Denon Network Receiver to SmartThings
 *		Tested with AVR-S710W (game1 & game2 inputs are not available), AVR 1912
 */

preferences {
    input("destIp", "text", title: "IP", description: "The device IP")
    input("destPort", "number", title: "Port", description: "The port you wish to connect", defaultValue: 80)
//    input("CBL", "text", title: "Set input name for SAT/CBL", description: "Set input for SAT/CBL", defaultValue: "SAT/CBL", required: false)
}

metadata {
    definition (name: "Denon AVR", namespace: "jlapenna", author: "Joe LaPenna") {
        capability "Actuator"
        capability "Switch" 
        capability "Polling"
        capability "Switch Level"
        capability "Music Player" 
        
        attribute "mute", "string"
        attribute "input", "string"
		attribute "inputChan", "enum"     
        
        command "mute"
        command "unmute"
        command "toggleMute"
        command "inputSelect", ["string"]
        command "inputNext"
		command "cbl"
		command "tv"
		command "BD"
		command "DVD"
		command "MP"
		command "BT"
		command "G1"
		command "G2"    
        }

    simulator {
        // TODO-: define status and reply messages here
    }

    //tiles {
	tiles(scale: 2) {
		multiAttributeTile(name:"multiAVR", type: "mediaPlayer", width: 6, height: 4) {
           tileAttribute("device.status", key: "PRIMARY_CONTROL") { 	            
            	attributeState ("paused", label: 'Paused', backgroundColor: "#53a7c0", defaultState: true)
				attributeState ("playing", label: 'Playing', backgroundColor: "#79b821")
        	}             
            tileAttribute("device.status", key: "MEDIA_STATUS") { 	            
            	attributeState "playing", label: '${name}', action:"switch.off"
                attributeState "paused", label: '${name}', action:"switch.on"
			}  
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
           		attributeState ("level", action:"setLevel")
                }       
            tileAttribute ("device.mute", key: "MEDIA_MUTED") {
            	attributeState("unmuted", action:"mute", nextState: "muted")
            	attributeState("muted", action:"unmute", nextState: "unmuted")
        	}
            tileAttribute("device.input", key: "MARQUEE") {
            	attributeState ("default", label:'${currentValue}', defaultState: true)
        	}
        }        
		standardTile("poll", "device.poll", width: 2, height: 2, decoration: "flat") {
            state "poll", label: "", action: "polling.poll", icon: "st.secondary.refresh", backgroundColor: "#FFFFFF"
        }
        standardTile("CBL", "device.switch", width: 2, height: 2, decoration: "flat"){
            state "Cable ON", label: 'Cable', action: "cbl", icon:"st.Electronics.electronics3" , backgroundColor: "#53a7c0"         
            state "Cable", label: 'Cable', action: "cbl", icon:"st.Electronics.electronics3" , backgroundColor: "#79b821"         
        	
            }
        standardTile("TV", "device.switch", width: 2, height: 2, decoration: "flat"){
        	state "TV", label: 'TV', action: "tv", icon:"st.Electronics.electronics18"
            }
        standardTile("BD", "device.switch", width: 2, height: 2, decoration: "flat"){
        	state "BD", label: 'Blu-ray', action: "BD", icon:"st.Electronics.electronics8"
        	}
        standardTile("DVD", "device.switch", width: 2, height: 2, decoration: "flat"){
        	state "DVD", label: 'Chromecast', action: "DVD", icon:"st.Electronics.electronics14"
        	}
		standardTile("MP", "device.switch", width: 2, height: 2, decoration: "flat"){
        	state "Media Player", label: 'Amazon TV', action: "MP", backgroundColor: "#ffffff", icon:"st.Electronics.electronics9"
			}
        standardTile("BT", "device.switch", width: 2, height: 2, decoration: "flat"){
        	state "BT", label: 'Bluetooth', action: "BT", icon:"st.Entertainment.entertainment2"
			}
		standardTile("sMode", "device.switch", width: 4, height: 2, decoration: "flat"){
        	state "sMusic", label: 'Music', action:"sMusic", icon:"st.Office.office12"
        	state "sMovie", label: 'Movie', action:"sMovie", icon:"st.Office.office12"
            }       

main "multiAVR"
        details(["multiAVR", "CBL", "TV", "BD","DVD", "MP", "BT","sMode","poll"])
    }
}
def parse(String description) {
	//log.debug "Parsing '${description}'"
    
 	def map = stringToMap(description)

    
    if(!map.body || map.body == "DQo=") { return }
        //log.debug "${map.body} "
	def body = new String(map.body.decodeBase64())
    
	def statusrsp = new XmlSlurper().parseText(body)
	def power = statusrsp.Power.value.text()

	if(power == "ON") { 
    	sendEvent(name: "status", value: 'playing') 
    }
    if(power != "" && power != "ON") { 
    	sendEvent(name: "status", value: 'paused')
	}
    
    def muteLevel = statusrsp.Mute.value.text()
    if(muteLevel == "on") { 
    	sendEvent(name: "mute", value: 'muted')
	}
    if(muteLevel != "" && muteLevel != "on") {
	    sendEvent(name: "mute", value: 'unmuted')
    }
    
    def inputCanonical = statusrsp.InputFuncSelect.value.text()
            sendEvent(name: "input", value: inputCanonical)
	        log.debug "Current Input is: ${inputCanonical}"
    
    def inputSurr = statusrsp.selectSurround.value.text() //Not used
	        log.debug "Current Surround is: ${inputSurr}"

    if(statusrsp.MasterVolume.value.text()) { 
    	def int volLevel = (int) statusrsp.MasterVolume.value.toFloat() ?: -40.0
       //volLevel = (volLevel + 80) * 0.9
        volLevel = (volLevel + 80)
        	log.debug "Adjusted volume is ${volLevel}"
   		
        def int curLevel = 36
        try {
        	curLevel = device.currentValue("level")
        	log.debug "Current volume is ${curLevel}"
        } catch(NumberFormatException nfe) { 
        	curLevel = 36
        }
	
        if(curLevel != volLevel) {
    		sendEvent(name: "level", value: volLevel)
        }
    } 
}


def setLevel(val) {
	sendEvent(name: "mute", value: "unmuted")     
    sendEvent(name: "level", value: val)
	def int scaledVal = val - 80
    request("cmd0=PutMasterVolumeSet%2F$scaledVal")
}

def on() {
	sendEvent(name: "status", value: 'playing')
	request('cmd0=PutZone_OnOff%2FON')
}

def off() { 
	sendEvent(name: "status", value: 'paused')
	request('cmd0=PutZone_OnOff%2FOFF')
}

def mute() { 
	sendEvent(name: "mute", value: "muted")
	request('cmd0=PutVolumeMute%2FON')
}

def unmute() { 
	sendEvent(name: "mute", value: "unmuted")
	request('cmd0=PutVolumeMute%2FOFF')
}

def toggleMute(){
    if(device.currentValue("mute") == "muted") { unmute() }
	else { mute() }
}

def cbl() {
	def cbl = "SAT/CBL"
    log.debug "Setting input to '${cbl}'"
    request("cmd0=PutZone_InputFunction%2FSAT/CBL")
    
	}


def tv() {
    log.debug "Setting input to TV Audio"
    request("cmd0=PutZone_InputFunction%2FTV")
    }

def BD() {
	log.debug "Setting input to Blu-ray"
    request("cmd0=PutZone_InputFunction%2FBD")
	}

def DVD() {
	log.debug "Setting input to Blu-ray"
    request("cmd0=PutZone_InputFunction%2FDVD")
	}

def MP() {
	log.debug "Setting input to Media Player"
    request("cmd0=PutZone_InputFunction%2FMPLAY")

	}

def BT() {
	log.debug "Setting input to Bluetooth"
    request("cmd0=PutZone_InputFunction%2FBT")
}
	
def sGame() {
	log.debug "Setting surround to GAME"
    request("cmd0=PutZone_InputFunction%2FGAME")
}
def sMusic() {
	log.debug "Setting surround to Music"	
    request("cmd0=PutSurroundMode%2F$MUSIC")
}

def sMovie() { 
	log.debug "Setting surround to Movie"
request("cmd0=PutSurroundMode%2F$MOVIE")
}

def sPure() {
	log.debug "Setting surround to Pure"
    request("cmd0=PutZone_InputFunction%2FPURE DIRECT")
}

def poll() { 
	//log.debug "Polling requested"
    refresh()
}

def refresh() {

    def hosthex = convertIPtoHex(destIp)
    def porthex = convertPortToHex(destPort)
    device.deviceNetworkId = "$hosthex:$porthex" 

    def hubAction = new physicalgraph.device.HubAction(
   	 		'method': 'GET',
    		'path': "/goform/formMainZone_MainZoneXml.xml",
            'headers': [ HOST: "$destIp:$destPort" ] 
		)   
     
   
    hubAction
}

def request(body) { 

    def hosthex = convertIPtoHex(destIp)
    def porthex = convertPortToHex(destPort)
    device.deviceNetworkId = "$hosthex:$porthex" 

    def hubAction = new physicalgraph.device.HubAction(
   	 		'method': 'POST',
    		'path': "/MainZone/index.put.asp",
        	'body': body,
        	'headers': [ HOST: "$destIp:$destPort" ]
		) 
              
    hubAction
}


private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04X', port.toInteger() )
    return hexport
}
