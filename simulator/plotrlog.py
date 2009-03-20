#!/usr/bin/python

import csv
import getopt
import re
import sys
import time

int_pattern = re.compile("^[-+]?[0-9]+$")
float_pattern = re.compile(r"^[-+]?[0-9]*\.?[0-9]+[eE]?[0-9]*$")

free_variables = ['Happiness', 'System Std', 'Voter Happiness Std', 'Gini']

class column(object):
	def __init__(self, name=None):
		self.is_int = True
		self.is_float = True
		self.name = name
		self.data = []
		self.numvals = None
	
	def append(self, x):
		if x is None:
			return
		# don't care what type it is yet
		self.data.append(x)
		if self.is_int and not int_pattern.match(x):
			self.is_int = False
		if self.is_float and not float_pattern.match(x):
			self.is_float = False
	
	def cleanup(self):
		if self.is_int:
			self.data = map(int, self.data)
			return
		if self.is_float:
			self.data = map(float, self.data)
			return

	def getUniqueValues(self):
		h = {}
		for x in self.data:
			h[x] = 1
		out = h.keys()
		self.numvals = len(out)
		return out
		
	def getNumValues(self):
		if self.numvals is None:
			self.getUniqueValues()
		return self.numvals


def plot(out, xcol, ycol, setcol):
	sets = {}
	for s,x,y in zip(setcol, xcol, ycol):
		sets[x] = (x,y)
	for sk, sd in sets.iteritems():
		out.write("%s\n" % sk)
		for x,y in sd:
			out.write("%s\t%s\n" % (x, y))
		out.write("\n")
	out.flush()

def getPlotSets(xname, yname, setsname, colhash,
	  filterargs=None, filterfunc=None):
	"""Plot based on data in hash of columns.

	Args:
	  xname: name of column in colhash for x axis
	  yname: name of column in colhash for y axis
	  setsname: name of column in colhash for data series
	  filterargs: list of names in colhash for arguments to filterfunc
	  filterfunc([]): given data from columns (in a list), return true if data should be plotted.

	Return: hash based on sets. {name: [list of (x,y) tuples]}
	"""
	sets = {}
	xcol = colhash[xname]
	if xcol is None:
		raise Error("invalid column name \"%s\"" % xname)
	ycol = colhash[yname]
	if ycol is None:
		raise Error("invalid column name \"%s\"" % yname)
	scol = colhash[setsname]
	if scol is None:
		raise Error("invalid column name \"%s\"" % setsname)
	for i in xrange(0, len(xcol.data)):
		if filterargs and filterfunc:
			arglist = [colhash[x].data[i] for x in filterargs]
			if not filterfunc(arglist):
				continue
		setkey = scol.data[i]
		setlist = None
		if setkey not in sets:
			setlist = []
			sets[setkey] = setlist
		else:
			setlist = sets[setkey]
		setlist.append( (xcol.data[i], ycol.data[i]) )
	return sets

def gnuplotSets(out, sets):
	setnames = sets.keys()
	print "plotting sets: " + ", ".join(setnames)
	def settitle(sn):
		return "'-' title \"%s\"" % sn
	titlepart = ", ".join(map(settitle, setnames))
	out.write("plot %s\n" % titlepart)
	for sn in setnames:
		setdata = sets[sn]
		for x,y in setdata:
			out.write("%s\t%s\n" % (x,y))
		out.write("e\n")
	out.write("\n")

def nmax(*a):
	"""My max function always prefers a value over None."""
	max = None
	for i in a:
		if (max is None) or ((i is not None) and (i > max)):
			max = i
	return max

def nmin(*a):
	"""My min function always prefers a value over None."""
	min = None
	for i in a:
		if (min is None) or ((i is not None) and (i < min)):
			min = i
	return min

svg_prologue = """<?xml version="1.0" standalone="no"?>
<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
<svg version="1.1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1600 1000">
"""

svg_colors = [
	'#00ff00', '#0000ff',
	'#800080', '#00ffff', '#ff0000',
	'#008000', '#ffa500', '#808000',
	'#000080', '#808080', '#800000',
	'#000000', '#ff00ff', '#ffff00',
	]

def svgSets(out, sets, xlabel=None, ylabel=None):
	setnames = sets.keys()
	ytop = 50
	ybottom = 900
	xleft = 100
	xright = 1300
	text_height = 21
	minx = None
	maxx = None
	miny = None
	maxy = None
	avhighs = {}
	for sn in setnames:
		sumy = 0.0
		county = 0
		for x,y in sets[sn]:
			minx = nmin(minx, x)
			maxx = nmax(maxx, x)
			miny = nmin(miny, y)
			maxy = nmax(maxy, y)
			sumy += y
			county += 1
		avhighs[sn] = sumy / county
	setnames.sort(lambda a,b: cmp(avhighs[a], avhighs[b]), reverse=True)
	print "plotting sets: " + ", ".join(setnames)
	xmult = (xright - xleft) / (maxx - minx)
	ymult = (ytop - ybottom) / (maxy - miny)
	gx = None
	gy = None
	out.write("""<g font-size="%d">\n""" % text_height)
	out.write("""<text x="%d" y="%d" alignment-baseline="middle" text-anchor="end">%s</text>""" %
		(xleft - 5, ytop, maxy))
	out.write("""<path fill="none" stroke-width="1" stroke="black" d="M %d %d L %d %d" />\n""" %
		(xleft - 4,ytop, xleft,ytop))
	out.write("""<text x="%d" y="%d" alignment-baseline="middle" text-anchor="end">%s</text>""" %
		(xleft - 5, ybottom, miny))
	out.write("""<path fill="none" stroke-width="1" stroke="black" d="M %d %d L %d %d" />\n""" %
		(xleft - 4,ybottom, xleft,ybottom))
	xvals = {}
	colori = -1
	for sn in setnames:
		setdata = sets[sn]
		colori = (colori + 1) % len(svg_colors)
		out.write("""<path fill="none" stroke-width="1" stroke="%s" d="M"""
			% svg_colors[colori])
		first = True
		for x,y in setdata:
			gx = ((x - minx) * xmult) + xleft
			gy = ((y - miny) * ymult) + ybottom
			out.write(" %d %d" % (gx,gy))
			if first:
				first = False
				out.write(" L")
			xvals[x] = 1
		out.write("\" />\n")
		if False:
			out.write("""<text x="%d" y="%d" alignment-baseline="middle" stroke="%s">%s</text>\n""" %
				(gx, gy, svg_colors[colori], sn))
	lx = xright
	ly = ytop
	colori = -1
	for sn in setnames:
		colori = (colori + 1) % len(svg_colors)
		out.write("""<text x="%d" y="%d" alignment-baseline="middle" fill="%s">%s</text>\n""" %
			(lx, ly, svg_colors[colori], sn))
		ly += text_height * (9/7)
	lastgx = None
	xvallist = xvals.keys();
	xvallist.sort()
	for x in xvallist:
		gx = ((x - minx) * xmult) + xleft
		out.write("""<path fill="none" stroke-width="1" stroke="black" d="M %d %d L %d %d" />\n""" %
			(gx, ybottom, gx, ybottom - 5))
		if (lastgx is not None) and ((gx - lastgx) < (text_height * 2)):
			continue
		out.write("""<text x="%d" y="%d" alignment-baseline="text-before-edge" text-anchor="middle">%s</text>\n""" %
			(gx, ybottom, x))
		lastgx = gx
	if xlabel is not None:
		out.write("""<text x="%d" y="%d" alignment-baseline="text-before-edge" text-anchor="middle" font-size="180%%">%s</text>\n""" %
			(((xright + xleft) / 2), ybottom + text_height, xlabel))
	if ylabel is not None:
		out.write("""<text alignment-baseline="text-before-edge" text-anchor="middle" font-size="180%%" transform="translate(%d, %d) rotate(90)">%s</text>\n""" %
			(xleft - text_height, ((ybottom + ytop) / 2), ylabel))
	out.write("""</g>\n""")

noshowMethods = [
	"Borda, truncated",
	"Maximized Rating Summation",
	"Rating Summation, 1..num choices",
	"Rating Summation, 1..10",
	"Instant Runoff Normalized Ratings, positive shifted",
	]

def eqbut(eq):
	def tmfu(x):
		for i in xrange(0,len(eq)):
			if x[i] != eq[i]:
				return False
		if x[-1] in noshowMethods:
			return False
		return True
	return tmfu

def product(l):
	return reduce(lambda x,y: x * y, l)

def permute_choices(l, choose):
	"""Generate lists of $choose elements from $l."""
	indecies = range(0,choose)
	while indecies[0] < len(l) - choose + 1:
		yield [l[i] for i in indecies]
		pos = choose - 1
		while pos < choose:
			indecies[pos] += 1
			# if indecies[choose - 1] >= len(l)
			# if indecies[choose - 2] >= len(l) - 1
			if indecies[pos] >= len(l) - ((choose - 1) - pos):
				pos -= 1
				if pos < 0:
					raise StopIteration
			else:
				pos += 1
				while pos < choose:
					indecies[pos] = indecies[pos - 1] + 1
					pos += 1

def permute_sets(*sets):
	"""Generate lists of combinations of elements from lists in sets."""
	iterators = [x.__iter__() for x in sets]
	values = [x.next() for x in iterators]
	while True:
		yield list(values)
		pos = 0
		goForward = True
		while goForward:
			try:
				values[pos] = iterators[pos].next()
				goForward = False
			except StopIteration:
				pos += 1
			if pos >= len(iterators):
				raise StopIteration
		while pos > 0:
			pos -= 1
			iterators[pos] = sets[pos].__iter__()
			values[pos] = iterators[pos].next()

def stepSteppable(steppable_columns, f):
	"""Run f(xaxis, setaxis, [others]) over columns list steppable_columns"""
	for xaxis in steppable_columns:
		if not xaxis.is_float:
			continue
		notx = list(steppable_columns)
		notx.remove(xaxis)
		for setaxis in notx:
			graphsets = list(notx)
			graphsets.remove(setaxis)
			f(xaxis, setaxis, graphsets)

def generateSteppable(steppable_columns):
	"""yield (xaxis, setaxis, [others]) over columns list steppable_columns"""
	for xaxis in steppable_columns:
		if not xaxis.is_float:
			continue
		notx = list(steppable_columns)
		notx.remove(xaxis)
		for setaxis in notx:
			graphsets = list(notx)
			graphsets.remove(setaxis)
			yield (xaxis, setaxis, graphsets)


def filename_sanitize(x):
	return x.replace(" ", "_").replace(",", "_")

def csvToColumnList(csvFile):
	cr = csv.reader(csvFile)
	headerRow = cr.next()
	columnlist = map(column, headerRow)
	rowcount = 0
	for row in cr:
		def ca(col, x):
			if col is None:
				return
			col.append(x)
		map(ca, columnlist, row)
	for c in columnlist:
		c.cleanup()
	return columnlist

def main(argv):
	# "rlog.csv"
	finame = None
	xaxis_opt = None
	setaxis_opt = None
	restricts = []
	optlist, args = getopt.gnu_getopt(argv[1:], "i:x:", ['in=', 'only=', 'x=', 'set='])
	for option, value in optlist:
		if option == '-i' or option == '--in':
			if finame is None:
				finame = value
			else:
				sys.stderr.write("multiple inputs specified but only one allowed\n")
				sys.exit(1)
		elif option == '-x' or option == '--x':
			xaxis_opt = value
			print "limiting to x axis \"%s\"" % xaxis_opt
		elif option == 'set':
			setaxis_opt = value
		elif option == 'only':
			restricts.append(value.split("="))
		else:
			sys.stderr.write("bogus option=\"%s\" value=\"%s\"\n" % (option, value))
			sys.exit(1)
	if finame is None:
		if len(args) == 1:
			finame = args[0]
		elif len(args) > 1:
			sys.stderr.write("multiple inputs specified but only one allowed\n")
			sys.exit(1)
		else:
			sys.stderr.write("no input specified. use -i/--in\n")
			sys.exit(1)
	start_time = time.time()
	columnlist = csvToColumnList(open(finame,"rb"))
	parse_end_time = time.time()
	dt = parse_end_time - start_time
	print "loaded %d rows in %f seconds" % (len(columnlist[0].data), dt)
	
	colhash = {}
	steppable_columns = []
	for x in columnlist:
		colhash[x.name] = x
		if x.name in free_variables:
			print "%s (int=%s, float=%s): free" % (x.name, x.is_int, x.is_float)
		else:
			uvals = x.getUniqueValues()
			if len(uvals) > 30:
				uvalstr = "(%d values)" % len(uvals)
			else:
				uvalstr = ", ".join(map(str, uvals))
				if len(uvals) > 1:
					steppable_columns.append(x)
			print "%s (int=%s, float=%s): %s" % (x.name, x.is_int, x.is_float, uvalstr)
	print ""
	print "steppable columns: " + ", ".join(map(lambda x: x.name, steppable_columns))
	
	graph_combo_count = 0
	for (xaxis, setaxis, graphsets) in generateSteppable(steppable_columns):
		if (xaxis_opt is not None) and (xaxis_opt != xaxis.name):
			continue
		if (setaxis_opt is not None) and (setaxis_opt != setaxis.name):
			continue
		graph_combos = product(map(lambda x: x.getNumValues(), graphsets))
		graph_combo_count += graph_combos
		print "x axis = %s, sets = %s: %d graphs over {%s}" % (
			xaxis.name, setaxis.name, graph_combos,
			", ".join(map(lambda x: x.name, graphsets))
			)
		dirname_spaces = "x %s l %s" % (xaxis.name, setaxis.name)
		dirname = filename_sanitize(dirname_spaces)
#		print dirname
	print "total graphs: %d" % graph_combo_count
	return
#	for column_combo in permute(steppable_columns, len(steppable_columns) - 1):
#		graph_combo_count += product(map(lambda x: x.getNumValues(), column_combo))
#	print "steppable combinations: %d" % product(map(lambda x: x.getNumValues(), steppable_columns))
#	print "columns: %s\n" % ", ".join(colhash.keys())
	sets = getPlotSets(
		"Error", "Happiness", "System", colhash,
		["Voters", "Choices", "System"], eqbut([100, 7]))
	# lambda a: (a[0:2] == [1000, 7]) and (a[2] not in nowshowMethods)
	if False:
		gpf = open("r.gnuplot","w")
		gpf.write("""set style data linespoints
	set terminal png
	""")
		gpf.write("""set output 'v1000c7.png'\n""")
		gnuplotSets(gpf, sets)
		gpf.close()
	svgout = open("v1000c7.svg", "w")
	svgout.write(svg_prologue)
	svgSets(svgout, sets, "Error", "Happiness")
	svgout.write("</svg>\n")
	svgout.close()

if __name__ == "__main__":
	main(sys.argv)
