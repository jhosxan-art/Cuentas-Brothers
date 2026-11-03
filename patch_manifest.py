# Agrega al AndroidManifest el filtro para que la app aparezca en "Compartir"
p = 'android/app/src/main/AndroidManifest.xml'
s = open(p, encoding='utf-8').read()
f = open('share-filter.xml', encoding='utf-8').read()

if 'android.intent.action.SEND' not in s:
    s = s.replace('</activity>', f + '\n        </activity>', 1)
    open(p, 'w', encoding='utf-8').write(s)
    print('Manifest actualizado')
else:
    print('Manifest ya tenia el filtro SEND')
