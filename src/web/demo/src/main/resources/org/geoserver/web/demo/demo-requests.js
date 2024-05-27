/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */


//when the user presses the "submit" button, make sure GET/POST request
function submitRequest() {
    var url =    document.getElementById("requestUrl").value.trim();
    //you cannot directly access the CodeMirror value in JS.  If you
    // try, you will get the original (on page load) value, not what
    // the user might have modified.
    var xml = document.gsEditors.requestBody.getValue().trim();

    var username = document.getElementsByName("username")[0].value;
    var password = document.getElementsByName("password")[0].value;

    if (url =="") {
        alert("Please enter a URL!");
        return; //nothing to do - no url
    }
    try{
        var _url = new URL(url);
    }
    catch(e) {
        alert("URL is is not valid!")
        return;
    }

    if (document.getElementById("openNewWindow").checked) {
        openInNewWindow(url, xml, username, password);
    }
    else {
        makeRequest(url, xml, username, password);
    }
}

function openInNewWindow(url,xml,user,pass) {
    url = addAuth(url, user,pass);
    if (xml && xml.trim() != "") {
        return openInNewWindowPost(url,xml);
    }
    return openInNewWindowGet(url);
}

//open new window with GET
// (just set the browser lcoation);
function openInNewWindowGet(url) {
    window.location.href = url;
}

//creates new URL by adding user/pass
//ie. http://user:pass@localhost:8080/geoserver/...
function addAuth(url, user,pass) {
    if (!user || !pass || (user == "") || (pass==""))
        return url;
    var _url = new URL(url);
    if (!_url.host || _url.host =="")
        return url;
    _url.username = user;
    _url.password = pass;
    return _url.toString();
}

function openInNewWindowPost(url,xml,user,pass) {
    let formElement = document.createElement("form");
    formElement.setAttribute("id", "formElement");
    formElement.setAttribute("action", url);
    formElement.setAttribute("method", "POST");
    formElement.setAttribute("enctype", "text/plain");
    formElement.style.display = "none";

    //HACK.
    // we break the XML into two piece, based on the first "="
    // xml1 = before the "=", xml2 = everthing after the "="
    // when submitting, browser will create:
    // name=value in the post body.
    // in our case, name=xml1 and value=xml2
    // this will re-build the xml in the body.
    var firstEquals = xml.indexOf("=");
    var xml1, xml2;
    if (firstEquals != -1) {
        xml1 = xml.substring(0, xml.indexOf("="))
        xml2 = xml.substring( xml.indexOf("=")+1);
    }
    else {
        //no "=" in
        window.alert("XML must have '=' somewhere in the text.  Please add an '='.");
        return;
    }

    var xmlInput = document.createElement("input");
    xmlInput.setAttribute("type", "text");
    xmlInput.setAttribute("name", xml1);
    xmlInput.setAttribute("value", xml2);

    formElement.append(xmlInput)

    var bodyElement =  document.getElementsByTagName("body")[0];
    var oldElement = document.getElementById("formElement");
    if (oldElement) {
        bodyElement.removeChild(oldElement);
    }
    bodyElement.append(formElement)
    formElement.submit();
}


//Make the actual XMLHttpRequest GET/POST Request
function makeRequest(url,xml,username,password) {
    // show spinner
    document.getElementById("ajaxFeedback").style.display = "block";

    let xhr = new XMLHttpRequest();
    var verb = "GET";
    if (xml && xml !== "") {
        verb = "POST";
    }
    xhr.open(verb, url, true,username,password);
    xhr.setRequestHeader( "Content-Type", "application/xml");
    xhr.responseType = 'arraybuffer';

    xhr.onreadystatechange = function () {
        //4 = complete
        if (this.readyState == 4)  {
            document.getElementById("ajaxFeedback").style.display = "none"
            handleResponse(this);
        }
    }
    // Sending our request
    if (xml && xml !== "") {
        xhr.send(xml);
    }
    else {
        xhr.send();
    }
}

function displayHeaders(xhrResponse) {
    var responseDiv = document.getElementById("responseHeadersDiv");

    //remove old content from the responseDiv
    while(responseDiv.firstChild){
        responseDiv.removeChild(responseDiv.firstChild);
    }
    let preElement = document.createElement("pre");
    preElement.innerHTML = xhrResponse.getAllResponseHeaders();
    responseDiv.append(preElement);
}

//given a complete response from the server, display it
//  error (turn border to red, process as normal)
// case 1 - image (convert to base64 Image representation and display)
// case 2 - other (XML - display the XML - might be pretty printed)
function handleResponse(xhrResponse) {
    var contentType = xhrResponse.getResponseHeader("Content-Type");
    var status = xhrResponse.status;

    //decode binary -> text
    var enc = new TextDecoder("utf-8");
    var text = enc.decode(xhrResponse.response);

    if (contentType.startsWith("image") && text.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")) {
        //this is actually a service exception that is tagged as an image
        contentType = "text/xml";
    }
    displayHeaders(xhrResponse);

    //error status
    if (status > 400) {
        document.getElementById("responseDiv").style.borderColor = 'red';
    }
    else {
        document.getElementById("responseDiv").style.borderColor = 'black';
    }
    //its an image - convert to Base64 representation and display in an <img> tag
    if (contentType.startsWith("image")) {
        //var base64Image = btoa(String.fromCharCode.apply(null, new Uint8Array(xhrResponse.response)));
        var base64Image = arrayBufferToBase64(xhrResponse.response);

        var responseDiv =    document.getElementById("responseDiv");
        //remove old content from the responseDiv
        while(responseDiv.firstChild){
            responseDiv.removeChild(responseDiv.firstChild);
        }

        //create the <img> and use the base64 image data
        let imgElement = document.createElement("img");
        imgElement.src = "data:"+contentType+";base64, "+base64Image;
        responseDiv.append(imgElement);
    }
    else {
        //its probably XML - process it for display in a <div><pre>...</pre><div>
        var responseDiv =    document.getElementById("responseDiv");
        //remove old content from the responseDiv
        while(responseDiv.firstChild){
            responseDiv.removeChild(responseDiv.firstChild);
        }

        //pretty print XML if its checked
        if (document.getElementById("prettyXML").checked) {
            const options = {indent: 2, newline: '\n'}
            text = prettifyXml(text, options);
        }

        //replace HTML/XML characters for display (and security)
        text=text.replaceAll("<","&lt;");
        text=text.replaceAll(">","&gt;");

        //create the <pre> element and imbed inside the <div>
        let preElement = document.createElement("pre");
        preElement.style.whiteSpace ="pre-wrap";
        preElement.innerHTML = text;
        responseDiv.append(preElement);
    }
}

function arrayBufferToBase64(buffer) {
    let binary = '';
    const bytes = new Uint8Array(buffer);
    for (let i = 0; i < bytes.byteLength; i++) {
        binary += String.fromCharCode(bytes[i]);
    }
    return btoa(binary);
}

function getBaseURL() {
    return window.location.href.substring(0, window.location.href.indexOf("web/wicket"));
}

function getOWSUrl() {
    return getBaseURL() + "ows?strict=true";
}


function getCoverage() {
    var url =getOWSUrl();
    var xml = document.getElementById("xml").value;
    var user ='';
    var pass= '';
    openInNewWindow(url,xml,user,pass);
}

function executeWPS() {
    var url =getOWSUrl();
    var xml = document.getElementById("xml").value;
    var user ='';
    var pass= '';
    if (document.getElementById('authenticate').checked) {
        user =document.getElementById('WPSusername').value;
        pass =document.getElementById('WPSpassword').value;
    }
    openInNewWindow(url,xml,user,pass);
}

