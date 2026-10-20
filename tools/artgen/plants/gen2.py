# Generated the M5 plant, crop and lick art and data rows (original pixel art; run from anywhere). Rerun only to regenerate untracked files.
import json, os, random, subprocess
from PIL import Image
ROOT=os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)),'../../..')); AS=ROOT+'/src/main/resources/assets/yearofice'
tracked=set(subprocess.run(['git','-C',ROOT,'ls-files','src/main/resources/assets'],capture_output=True,text=True).stdout.split())
def exists(rel): return 'src/main/resources/assets/yearofice/'+rel in tracked
def w(rel,obj):
    if exists(rel): return
    os.makedirs(os.path.dirname(f'{AS}/{rel}'),exist_ok=True); json.dump(obj,open(f'{AS}/{rel}','w'),indent=2)
def png(rel,img):
    if exists(rel): return
    os.makedirs(os.path.dirname(f'{AS}/{rel}'),exist_ok=True); img.save(f'{AS}/{rel}')
def noise(key,base,var=14,size=16):
    r=random.Random(key); img=Image.new('RGBA',(size,size))
    for x in range(size):
        for y in range(size):
            v=r.randint(-var,var); img.putpixel((x,y),tuple(max(0,min(255,c+v)) for c in base)+(255,))
    return img
# garden bed
png('textures/block/garden_bed.png',noise('bed',(96,72,52)))
png('textures/block/garden_bed_top.png',(lambda im:[im.putpixel((x,y),(70,52,36,255)) for x in range(16) for y in (3,4,9,10,15)] and im)(noise('bedt',(104,78,56))))
w('models/block/garden_bed.json',{'parent':'minecraft:block/cube_bottom_top','textures':{'top':'yearofice:block/garden_bed_top','bottom':'yearofice:block/garden_bed','side':'yearofice:block/garden_bed'}})
w('blockstates/garden_bed.json',{'variants':{'':{'model':'yearofice:block/garden_bed'}}})
w('models/item/garden_bed.json',{'parent':'yearofice:block/garden_bed'}) if False else None
# crops: three looks (sown/sprout, growing, ripe) + dead
crops={'potato':((70,130,60),(90,150,70),(60,110,50)),'barley':((120,170,70),(170,170,70),(205,185,90)),'wheat':((120,170,70),(190,170,70),(222,190,90)),'turnip':((80,140,70),(100,150,80),(190,120,160)),'cabbage':((90,150,90),(110,170,100),(150,200,120))}
def crop_img(name,look):
    r=random.Random(name+str(look)); img=Image.new('RGBA',(16,16),(0,0,0,0)); col=crops[name]
    if look==0:
        for _ in range(5): img.putpixel((r.randint(2,13),r.randint(13,15)),(120,100,70,255))
        return img
    h={1:4,2:8,3:11}.get(look,9)
    c=col[min(look,3)-1] if look<=3 else (110,95,70)
    xs=(4,8,12) if look<5 else (5,9,12)
    for x0 in xs:
        for y in range(16-h,16):
            img.putpixel((x0,y),c+(255,))
            if look>=2 and y%3==0 and x0+1<16: img.putpixel((x0+1,y),c+(255,))
    if look==3 and name in('barley','wheat'):
        for x0 in xs:
            for y in range(16-h-2,16-h+1): img.putpixel((x0,y),(222,196,100,255))
    return img
for n in crops:
    for look,rng in ((0,'0'),(1,'1'),(2,'2'),(3,'3'),(5,'5')):
        png(f'textures/block/{n}_crop_{rng}.png',crop_img(n,look))
        w(f'models/block/{n}_crop_{rng}.json',{'parent':'minecraft:block/cross','textures':{'cross':f'yearofice:block/{n}_crop_{rng}'},'render_type':'minecraft:cutout'})
    ms=[]
    for st,look in ((0,'0'),(1,'1'),(2,'2'),(3,'3'),(4,'3'),(5,'5')):
        ms.append({'when':{'stage':str(st)},'apply':{'model':f'yearofice:block/{n}_crop_{look}'}})
    w(f'blockstates/{n}_crop.json',{'multipart':ms})
# salt crust: 5 states
for st in range(5):
    base=(238,238,232) if st>0 else (150,140,120)
    img=noise(f'crust{st}',base,10)
    png(f'textures/block/salt_crust_{st}.png',img)
    w(f'models/block/salt_crust_{st}.json',{'parent':'minecraft:block/cube_all','textures':{'all':f'yearofice:block/salt_crust_{st}'}})
w('blockstates/salt_crust.json',{'variants':{f'crust={st}':{'model':f'yearofice:block/salt_crust_{st}'} for st in range(5)}})
# items
def item(name,col,shape='blob'):
    r=random.Random('item'+name); img=Image.new('RGBA',(16,16),(0,0,0,0))
    if shape=='tool':
        for i in range(3,14): img.putpixel((i,16-i-1+0),(110,80,50,255)); img.putpixel((i,16-i),(110,80,50,255))
        for x in range(2,7):
            for y in range(2,5): img.putpixel((x,y),col+(255,))
    elif shape=='quern':
        for x in range(2,14):
            for y in range(4,12):
                if (x-8)**2+(y-8)**2<=30: img.putpixel((x,y),tuple(c+r.randint(-10,10) for c in col)+(255,))
    else:
        for _ in range(3):
            cx,cy=r.randint(5,10),r.randint(6,10)
            for x in range(16):
                for y in range(16):
                    if (x-cx)**2+(y-cy)**2<=9 and r.random()<0.9: img.putpixel((x,y),tuple(max(0,min(255,c+r.randint(-14,14))) for c in col)+(255,))
    png(f'textures/item/{name}.png',img)
    w(f'models/item/{name}.json',{'parent':'minecraft:item/handheld' if shape=='tool' else 'minecraft:item/generated','textures':{'layer0':f'yearofice:item/{name}'}})
for n,c,s in [('hoe',(150,150,150),'tool'),('digging_stick',(170,140,100),'tool'),('shovel',(160,160,170),'tool'),('hand_quern',(150,148,140),'quern'),
  ('seed_potato',(190,160,100),'blob'),('barley_seed',(210,190,110),'blob'),('wheat_seed',(222,190,100),'blob'),('turnip_seed',(120,90,70),'blob'),('cabbage_seed',(140,120,90),'blob'),
  ('potato',(190,150,90),'blob'),('barley',(215,195,115),'blob'),('wheat',(225,195,105),'blob'),('turnip',(200,140,190),'blob'),('cabbage',(130,190,110),'blob'),
  ('grain_sheaf',(215,185,95),'blob'),('straw',(220,200,120),'blob'),('lick_crust',(235,235,228),'blob'),('lick_salt',(245,245,245),'blob')]:
    item(n,c,s)
