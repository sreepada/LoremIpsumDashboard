function httpGet(theUrl)
{
    var xmlHttp = null;

    xmlHttp = new XMLHttpRequest();
    xmlHttp.open( "GET", theUrl, false );
    xmlHttp.send();
    return xmlHttp.responseText;
}

var searchString = "location_s__s: *Artic Ocean*";
var my_data = httpGet("http://localhost:8983/solr/select?q=" + searchString + "&fl=id&wt=json&indent=true&facet=true&facet.pivot=location_s__s,year_created_s,science_keywords_s");
my_data = JSON.parse(my_data);
var i = 0;
finalJson = '{ "name": "' + searchString + '", "children": [ ';
reducedJson = my_data.facet_counts.facet_pivot["location_s__s,year_created_s,science_keywords_s"];
while (i < reducedJson.length) 
{
    finalJson += '{ "name" : "' + reducedJson[i].value + '", "children": [';
    var yearReducedJson = reducedJson[i].pivot;
    var j = 0;
    while (j < yearReducedJson.length) 
    {
        finalJson += '{ "name" : "' + yearReducedJson[j].value + '", "children": [';
        var keywordsReducedJson = yearReducedJson[j].pivot;
        var k = 0;
        var keywordMap = {};
        while (k < keywordsReducedJson.length) {
            var mySplitResult = keywordsReducedJson[k].value.trim().split(",");
            for (var l = 0; l < mySplitResult.length; l++) {
                if (keywordMap[mySplitResult[l]] != undefined) {
                    keywordMap[mySplitResult[l]] += keywordsReducedJson[k].count;
                }
                else {
                    keywordMap[mySplitResult[l]] = keywordsReducedJson[k].count;
                }
            }
            k++;
        }
        for (var scienceKeyword in keywordMap) {
            finalJson += '{ "name": "' + scienceKeyword + '", "size": ' + keywordMap[scienceKeyword] + '},';
        }
        finalJson = finalJson.replace(/,\s*$/, "") + ']},'
        j++;
    }
    finalJson = finalJson.replace(/,\s*$/, "") + ']},'
    i++;
}
finalJson = finalJson.replace(/,\s*$/, "") + ']}'
//console.log(finalJson);
