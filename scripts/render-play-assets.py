"""Rasterize the existing AX vector mark into Play listing assets (no secrets)."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'app/build/play-assets'
OUT.mkdir(parents=True, exist_ok=True)
INK, PAPER = '#191915', '#F3F0E7'
NS = '{http://schemas.android.com/apk/res/android}'
vector = ET.parse(ROOT / 'app/src/main/res/drawable/ic_launcher_foreground.xml').getroot()
paths = vector.findall('.//path')

def mark(image, left, top, scale, color):
    mask = Image.new('L', image.size)
    draw = ImageDraw.Draw(mask)
    for index, path in enumerate(paths):
        for contour_index, contour in enumerate(path.get(NS+'pathData').split('Z')):
            numbers = list(map(float, re.findall(r'-?\d+(?:\.\d+)?', contour)))
            if not numbers:
                continue
            points = [(left+(numbers[i]+(460 if index else 0))*scale,
                       top+(745-numbers[i+1])*scale) for i in range(0,len(numbers),2)]
            draw.polygon(points, fill=255 if contour_index == 0 else 0)
    image.paste(Image.new('RGB',image.size,color),(0,0),mask)

icon = Image.new('RGB',(512,512),INK)
mark(icon,97,155,.27,PAPER)
icon.save(OUT/'icon-512.png')

feature=Image.new('RGB',(1024,500),PAPER)
draw=ImageDraw.Draw(feature)
draw.line((56,48,968,48), fill=INK, width=2)
draw.line((56,452,968,452), fill=INK, width=2)
mark(feature,66,140,.29,INK)
fonts=ROOT/'app/src/main/res/font'
headline=ImageFont.truetype(str(fonts/'noto_serif_kr_semibold.otf'),56)
body=ImageFont.truetype(str(fonts/'ibm_plex_sans_kr_regular.ttf'),25)
small=ImageFont.truetype(str(fonts/'ibm_plex_sans_kr_medium.ttf'),19)
draw.text((470,133),'AX Notes',font=headline,fill=INK)
draw.text((474,225),'직접 해보고, 만들고, 기록하다.',font=body,fill=INK)
draw.text((474,278),'AI 활용의 경험과 시행착오',font=body,fill=INK)
draw.text((58,409),'READ  /  SAVE  /  REFLECT',font=small,fill=INK)
feature.save(OUT/'feature-1024x500.png')
for path in [OUT/'icon-512.png',OUT/'feature-1024x500.png']:
    with Image.open(path) as image:
        print(path.name,image.size,image.mode,path.stat().st_size)
