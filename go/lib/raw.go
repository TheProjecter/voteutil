package voting

import "strconv"

// Raw rating summation election.
type RawSummation struct {
	sum []float64
	Names *NameMap
}

func (it *RawSummation) Vote(vote NameVote) {
	if it.Names == nil {
		it.Names = new(NameMap)
	}
	for _, nv := range vote {
		x := it.Names.NameToIndex(nv.Name)
		for x < len(it.sum) {
			it.sum = append(it.sum, 0.0)
		}
		it.sum[x] += nv.Rating
	}
}

func (it *RawSummation) VoteIndexes(vote IndexVote) {
	for i, value := range vote.Ratings {
		x := vote.Indexes[i]
		for x < len(it.sum) {
			it.sum = append(it.sum, 0.0)
		}
		it.sum[x] += value
	}
}
func (it *RawSummation) GetWinners() *NameVote {
	out := new(NameVote)
	if it.Names != nil {
		for name, x := range it.Names.Indexes {
			var value float64
			if x < len(it.sum) {
				value = it.sum[x]
			} else {
				value = 0.0
			}
			*out = append(*out, NameRating{name, value})
			//(*out)[name] = value
		}
	} else {
		for x, value := range it.sum {
			*out = append(*out, NameRating{strconv.Itoa(x), value})
			//(*out)[strconv.Itoa(x)] = value
		}
	}
	return out
}
func (it *RawSummation) HtmlExlpaination() string {
	return ""
}
