#!/usr/bin/python
import sys
import requests
import os
import json
from collections import defaultdict

def get_bar_result(query):
    get_query = "http://localhost:8983/solr/select?q=%s&fl=id,location_s__s,date_created_s&rows=1000&wt=json&indent=true"%query
    r = requests.get(get_query)
    result = json.loads(r.content)
    num_results = int(result["response"]["numFound"])
    if num_results < 0:
        return json.dumps({})

    year_dict = defaultdict(int)

    for entry in result["response"]["docs"]:
        if "date_created_s" not in entry:
            continue

        date_created = entry["date_created_s"].strip()
        date = date_created.split()[0]
        year = date.split("-")[0]
        year_dict[year] = year_dict[year] + 1
    print year_dict

if __name__ == "__main__":
    chart_type = sys.argv[1]
    query = sys.argv[2]

    if chart_type == "bar":
        result = get_bar_result(query)
        print result


