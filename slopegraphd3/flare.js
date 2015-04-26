function httpGet(theUrl)                                                                                                                              
{
    var xmlHttp = null;

    xmlHttp = new XMLHttpRequest();
    xmlHttp.open( "GET", theUrl, false );
    xmlHttp.send();
    return xmlHttp.responseText;
}

var facet = " Arctic Ocean"
var my_data = httpGet("http://localhost:8983/solr/select?q=location_s__s:" + facet + "&fl=title,science_keywords_s&wt=json&indent=true&rows=1000");
my_data = JSON.parse(my_data);
var i = 0;
finalJson = '{ "name": "' + facet + '", "children": [ ';
while (i < my_data.response.docs.length) 
{
    if (my_data.response.docs[i].title[0] !== undefined && my_data.response.docs[i].science_keywords_s !== undefined) 
    {
        finalJson += '{ "name" : "' + my_data.response.docs[i].title[0] + '", "children": [';
        var mySplitResult = my_data.response.docs[i].science_keywords_s.split(",");
        for (var j = 0; j < mySplitResult.length; j++) {
            finalJson += '{ "name": "' + mySplitResult[j].trim() + '", "size": 100},'
        }
        finalJson = finalJson.replace(/,\s*$/, "") + ']},'
    }
    i++;
}
finalJson = finalJson.replace(/,\s*$/, "") + ']}'
console.log(finalJson);
