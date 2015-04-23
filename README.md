This is useful for Custom Combined Log Format like given below

220.168.132.72 - - [2014-07-19T16:05:44Z] "GET / HTTP/1.1" 503 1195 "-" "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322)"
176.177.122.33 - - [2014-07-19T16:05:44Z] "GET /services.html HTTP/1.1" 200 1087 "-" "Mozilla/5.0 (X11; Linux x86_64; rv:6.0a1) Gecko/20110421 Firefox/6.0a1"


and the expected output would be comma seperated values like this 

(110.210.238.224,-,-,2014-07-19T16:05:36Z],GET,/,HTTP/1.1",200,2137,-,Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322))
(189.72.254.96,-,-,2014-07-19T16:05:36Z],GET,/contactus.html,HTTP/1.1",200,1330,-,Mozilla/5.0 (X11; Linux x86_64; rv:6.0a1) Gecko/20110421 Firefox/6.0a1)

the output shown above may differ from the input since it is just a sample output taken from actual output but the format would be like this.

