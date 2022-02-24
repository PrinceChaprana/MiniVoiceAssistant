import wikipedia as wiki
import googletrans as gs
from googletrans import Translator

lang = {
    "english":"en",
    "spanish":"es",
    "chinese":"zh",
    "french":"fr",
    "arabic":"ar",
    "hindi":"hi"
}

def wikipediaSearch(query):
    query = query.lower()
    query = query.replace("","")
    results = wiki.summary(query,sentences = 2)
    return results
def retCodeforQry(query):
    query = query.lower()
    if('wikipedia' in query):
        query = query.replace('wikipedia',"1")
        return query
def translatorl(str,q):
    q = q.lower()
    q = q.split(" ")
    lang1 = lang[q[0]]
    lang2 = lang[q[2]]
    traslator = Translator()
    translatedtext = traslator.translate(str,src=lang1,dest=lang2)
    return translatedtext
