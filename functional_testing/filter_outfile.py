outfile_r=open('outfile', 'r')
filtered_r=open('filtered_outfile', 'w')

lines = outfile_r.readlines()

for line in lines:
    if line.startswith("APP"):
        filtered_r.write(line)

outfile_r.close()
filtered_r.close()